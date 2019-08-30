package org.getmonero.i2p.zero;

import net.i2p.I2PAppContext;
import net.i2p.app.ClientAppManager;
import net.i2p.app.ClientAppManagerImpl;
import net.i2p.client.I2PClient;
import net.i2p.client.I2PClientFactory;
import net.i2p.data.Base64;
import net.i2p.data.Destination;
import net.i2p.i2ptunnel.I2PTunnel;
import net.i2p.sam.SAMBridge;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TunnelControl implements Runnable {

  private RouterWrapper routerWrapper;
  private boolean stopping = false;
  private ServerSocket controlServerSocket;
  private File tunnelControlConfigDir;
  private File tunnelControlTempDir;
  private TunnelList tunnelList;

  private static final int TUNNEL_CONTROL_LISTENING_PORT = 8051;

  public static class TunnelList {
    private File tunnelControlConfigDir;
    private File tunnelControlTempDir;
    private RouterWrapper routerWrapper;
    private List<Tunnel> tunnels = new ArrayList<>();
    private List<ChangeListener<List<Tunnel>>> changeListeners = new ArrayList<>();

    public TunnelList(File tunnelControlConfigDir, File tunnelControlTempDir, RouterWrapper routerWrapper) {
      this.routerWrapper = routerWrapper;
      this.tunnelControlConfigDir = tunnelControlConfigDir;
      this.tunnelControlTempDir = tunnelControlTempDir;
    }

    public void addChangeListener(ChangeListener<List<Tunnel>> listener) {
      changeListeners.add(listener);
    }
    public void fireChangeEvent() {
      for(var listener : changeListeners) listener.onChange(tunnels);
    }

    public void addTunnel(Tunnel t) {
      tunnels.add(t);
      save();
      fireChangeEvent();
    }
    public void removeTunnel(Tunnel t) {
      tunnels.remove(t);
      save();
      fireChangeEvent();
    }
    public Stream<Tunnel> getTunnelsCopyStream() {
      return new ArrayList<>(tunnels).stream();
    }
    public void load() {
      try {
        File tunnelControlConfigFile = new File(tunnelControlConfigDir, "tunnels.json");
        if (tunnelControlConfigFile.exists()) {
          tunnels.clear();
          JSONObject root = (JSONObject) new JSONParser().parse(Files.readString(tunnelControlConfigFile.toPath()));
          JSONArray list = (JSONArray) root.get("tunnels");
          list.stream().forEach((o)->{
            JSONObject obj = (JSONObject) o;
            String type = (String) obj.get("type");
            switch (type) {
              case "server":
                tunnels.add(new ServerTunnel((String) obj.get("host"), Integer.parseInt((String) obj.get("port")), new KeyPair((String) obj.get("keypair")), tunnelControlTempDir, routerWrapper));
                break;
              case "eepsite":
                tunnels.add(new EepSiteTunnel((Boolean) obj.get("enabled"), new KeyPair((String) obj.get("keypair")), (String) obj.get("contentDir"), (String) obj.get("logsDir"), (Boolean) obj.get("allowDirectoryBrowsing"), (Boolean) obj.get("enableLogs"), Integer.parseInt((String) obj.get("port")), tunnelControlTempDir, routerWrapper));
                break;
              case "client":
                tunnels.add(new ClientTunnel((String) obj.get("dest"), Integer.parseInt((String) obj.get("port")), routerWrapper));
                break;
              case "socks":
                tunnels.add(new SocksTunnel(Integer.parseInt((String) obj.get("port")), routerWrapper));
                break;
              case "http":
                tunnels.add(new HttpClientTunnel(Integer.parseInt((String) obj.get("port")), routerWrapper));
                break;
            }
          });
          fireChangeEvent();
        }

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    public String getJSON(boolean includeKeyPairs, boolean includeState) {
      try {
        JSONObject root = new JSONObject();
        JSONArray tunnelsArray = new JSONArray();
        root.put("tunnels", tunnelsArray);
        for(Tunnel t : tunnels) {
          JSONObject entry = new JSONObject();
          entry.put("type", t.getType());
          if(includeState) entry.put("state", t.getState());
          tunnelsArray.add(entry);
          switch (t.getType()) {
            case "server":
            case "eepsite":
              entry.put("host", t.getHost());
              entry.put("port", t.getPort()+"");
              entry.put("dest", t.getI2P());
              if(includeKeyPairs) entry.put("keypair", ((ServerTunnel) t).keyPair.toString());
              if(t.getType().equals("eepsite")) {
                EepSiteTunnel eepSiteTunnel = (EepSiteTunnel) t;
                entry.put("enabled", eepSiteTunnel.enabled);
                entry.put("contentDir", eepSiteTunnel.contentDir);
                entry.put("logsDir", eepSiteTunnel.logsDir);
                entry.put("allowDirectoryBrowsing", eepSiteTunnel.allowDirectoryBrowsing);
                entry.put("enableLogs", eepSiteTunnel.enableLogs);
              }
              break;

            case "client":
              entry.put("dest", t.getI2P());
              entry.put("port", t.getPort()+"");
              break;

            case "socks":
            case "http":
              entry.put("port", t.getPort()+"");
              break;
          }
        }
        return root.toJSONString();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    public void save() {
      try {
        Files.writeString(new File(tunnelControlConfigDir, "tunnels.json").toPath(), getJSON(true, false));
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public TunnelControl(RouterWrapper routerWrapper, File tunnelControlConfigDir, File tunnelControlTempDir) {
    this.routerWrapper = routerWrapper;
    tunnelControlTempDir.delete();
    tunnelControlTempDir.mkdir();

    this.tunnelControlConfigDir = tunnelControlConfigDir;
    this.tunnelControlTempDir = tunnelControlTempDir;
    Runtime.getRuntime().addShutdownHook(new Thread(()->this.tunnelControlTempDir.delete()));

    tunnelList = new TunnelList(tunnelControlConfigDir, tunnelControlTempDir, routerWrapper);
  }

  public static abstract class Tunnel {
    RouterWrapper routerWrapper;
    public volatile I2PTunnel tunnel;
    public boolean enabled = true;
    public abstract String getType();
    public abstract String getHost();
    public abstract int getPort();
    public abstract String getI2P();
    public abstract Tunnel start();

    protected Tunnel(RouterWrapper routerWrapper) {
      this.routerWrapper = routerWrapper;
    }

    public String getState() {
      return tunnel==null ? "opening" : "open";
    }
    public boolean getEnabled() { return enabled; }
    public void destroy(boolean fastDestroy) {
      new Thread(()->{
        // tunnels may sleep for 20 seconds while waiting to open. we may be in a hurry
        if(!fastDestroy) {
          while(tunnel==null) { try { Thread.sleep(100); } catch (InterruptedException e) {} } // wait for tunnel to be established before closing it
        }
        if(tunnel!=null) {
          tunnel.runClose(new String[]{"forced", "all"}, tunnel);
        }
      }).start();
    }
  }

  public static class ClientTunnel extends Tunnel {
    public String dest;
    public int port;

    public ClientTunnel(String dest, int port, RouterWrapper routerWrapper) {
      super(routerWrapper);
      this.dest = dest;
      this.port = port;
    }

    @Override
    public Tunnel start() {
      new Thread(()->{
        routerWrapper.waitForRouterRunning();
        tunnel = new I2PTunnel(new String[]{"-die", "-nocli", "-e", "config localhost 7654", "-e", "client " + port + " " + dest});
      }).start();
      return this;
    }

    @Override public String getType() { return "client"; }
    @Override public String getHost() { return "localhost"; }
    @Override public int getPort() { return port; }
    @Override public String getI2P() { return dest; }

  }
  public static class HttpClientTunnel extends Tunnel {
    public int port;
    public HttpClientTunnel(int port, RouterWrapper routerWrapper) {
      super(routerWrapper);
      this.port = port;
    }

    @Override
    public Tunnel start() {
      new Thread(()->{
        routerWrapper.waitForRouterRunning();
        tunnel = new I2PTunnel(new String[]{"-die", "-nocli", "-e", "config localhost 7654", "-e", "httpclient " + port});
      }).start();
      return this;
    }

    @Override public String getType() { return "http"; }
    @Override public String getHost() { return "localhost"; }
    @Override public int getPort() { return port; }
    @Override public String getI2P() { return "n/a"; }
  }
  public static class ServerTunnel<T extends ServerTunnel> extends Tunnel {
    public String dest;
    public String host;
    public int port;
    public KeyPair keyPair;
    private File tunnelControlTempDir;
    public ServerTunnel(String host, int port, KeyPair keyPair, File tunnelControlTempDir, RouterWrapper routerWrapper) {
      super(routerWrapper);
      try {
        this.host = host;
        this.port = port;
        this.keyPair = keyPair;
        this.dest = keyPair.b32Dest;
        this.tunnelControlTempDir = tunnelControlTempDir;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public Tunnel start() {
      new Thread(() -> {
        routerWrapper.waitForRouterRunning();
        try {
          String uuid = new BigInteger(128, new Random()).toString(16);
          String seckeyPath = tunnelControlTempDir.getAbsolutePath() + File.separator + "seckey." + uuid + ".dat";

          Files.write(Path.of(seckeyPath), Base64.decode(keyPair.seckey));
          new File(seckeyPath).deleteOnExit(); // clean up temporary file that was only required because new I2PTunnel() requires it to be written to disk

          // listen using the I2P server keypair, and forward incoming connections to a destination and port
          tunnel = new I2PTunnel(new String[]{"-die", "-nocli", "-e", "server " + host + " " + port + " " + seckeyPath});
        }
        catch(Exception e) {
          throw new RuntimeException(e);
        }
      }).start();
      return this;
    }

    @Override public String getType() { return "server"; }
    @Override public String getHost() { return host; }
    @Override public int getPort() { return port; }
    @Override public String getI2P() { return dest; }

  }

  public static class EepSiteTunnel extends ServerTunnel {
    public Server server;
    public String contentDir;
    public String logsDir;
    public Boolean allowDirectoryBrowsing;
    public Boolean enableLogs;
    public EepSiteTunnel(boolean enabled, KeyPair keyPair, String contentDirStr, String logsDirStr, boolean allowDirectoryBrowsing, boolean enableLogs, int port, File tunnelControlTempDir, RouterWrapper routerWrapper) {
      super("localhost", port, keyPair, tunnelControlTempDir, routerWrapper);

      this.enabled = enabled;
      this.contentDir = contentDirStr;
      this.logsDir = logsDirStr;
      this.allowDirectoryBrowsing = allowDirectoryBrowsing;
      this.enableLogs = enableLogs;
      this.port = port;
      this.keyPair = keyPair;
    }

    @Override
    public void destroy(boolean fastDestroy) {
      try {
        server.stop();
        super.destroy(fastDestroy);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }

    }

    public void stopJetty() {
      if(server==null) return;
      try {
        server.stop();
        server = null;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public void startJetty() {
      try {

        File contentDirFile = new File(contentDir);
        File logsDirFile = new File(logsDir);

        contentDirFile.mkdirs();
        logsDirFile.mkdirs();

        int maxThreads = 100;
        int minThreads = 1;
        int idleTimeout = 120;
        QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleTimeout);

        server = new Server(threadPool);
        server.setStopAtShutdown(true);

        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSendServerVersion(false);
        httpConfig.setSendDateHeader(false);
        httpConfig.setSendXPoweredBy(false);
        httpConfig.setSendServerVersion(false);

        HandlerList handlers = new HandlerList();
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(allowDirectoryBrowsing);
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});
        resourceHandler.setResourceBase(contentDirFile.getAbsolutePath());
        handlers.addHandler(resourceHandler);
        server.setHandler(handlers);

        if(enableLogs) {
          NCSARequestLog requestLog = new NCSARequestLog(logsDirFile.getAbsolutePath() + File.separator + "eepsite-yyyy_mm_dd.request.log");
          requestLog.setAppend(true);
          requestLog.setExtended(false);
          requestLog.setLogTimeZone("UTC");
          requestLog.setLogLatency(true);
          requestLog.setRetainDays(0);
          server.setRequestLog(requestLog);
        }

        ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        http.setPort(port);
        http.setIdleTimeout(60000);
        http.setHost("localhost");
        server.addConnector(http);

        server.start();
        server.join();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public Tunnel start() {
      if(!enabled) return this;
      new Thread(()->startJetty()).start();
      return super.start();
    }


    @Override public String getType() { return "eepsite"; }

  }

  public static class SocksTunnel extends Tunnel {
    public int port;
    public SocksTunnel(int port, RouterWrapper routerWrapper) {
      super(routerWrapper);
      this.port = port;
    }

    @Override
    public Tunnel start() {
      new Thread(()->{
        routerWrapper.waitForRouterRunning();
        tunnel = new I2PTunnel(new String[]{"-die", "-nocli", "-e", "sockstunnel " + port});
      }).start();
      return this;
    }

    @Override public String getType() { return "socks"; }
    @Override public String getHost() { return "localhost"; }
    @Override public int getPort() { return port; }
    @Override public String getI2P() { return "n/a"; }
  }

  public TunnelList getTunnelList() {
    return tunnelList;
  }

  public static class KeyPair {
    public String seckey;
    public String pubkey;
    public String b32Dest;
    public KeyPair(String seckey, String pubkey, String b32Dest) {
      this.seckey = seckey;
      this.pubkey = pubkey;
      this.b32Dest = b32Dest;
    }

    @Override
    public String toString() {
      return seckey + "," + pubkey;
    }

    public KeyPair(String base64EncodedCommaDelimitedPair) {
      try {
        String[] a = base64EncodedCommaDelimitedPair.split(",");
        this.seckey = a[0];
        this.pubkey = a[1];
        Destination d = new Destination();
        d.readBytes(new ByteArrayInputStream(Base64.decode(this.seckey)));
        this.b32Dest = d.toBase32();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    public static KeyPair gen() {
      try {
        ByteArrayOutputStream seckey = new ByteArrayOutputStream();
        ByteArrayOutputStream pubkey = new ByteArrayOutputStream();
        I2PClient client = I2PClientFactory.createClient();
        Destination d = client.createDestination(seckey);
        d.writeBytes(pubkey);
        String b32Dest = d.toBase32();
        return new KeyPair(Base64.encode(seckey.toByteArray()), Base64.encode(pubkey.toByteArray()), b32Dest);
      }
      catch (Exception e){
        throw new RuntimeException(e);
      }
    }
    public static KeyPair read(String path) throws Exception {
      return new KeyPair(Files.readString(Paths.get(path)));
    }
    public void write(String path) throws Exception {
      Files.writeString(Paths.get(path), toString());
    }

  }
  
  public boolean isPortAlreadyAssigned(int port) {
    return tunnelList.tunnels.stream().anyMatch(t->!(t instanceof ServerTunnel) && t.getPort()==port);
  }

  static class KeyPairHolder {
    public KeyPair keyPair = null;
  }
  public static KeyPair generateVanityKeypair(String vanityPrefix) {

    if(vanityPrefix.length()>3) vanityPrefix = vanityPrefix.substring(0, 3);
    vanityPrefix = vanityPrefix.toLowerCase();
    for(int i=0; i<vanityPrefix.length(); i++) {
      // reject vanityPrefix if not alphanumeric, since b32 addresses can only be alphanumeric
      char c = vanityPrefix.charAt(i);
      if(!(Character.isAlphabetic(c) || Character.isDigit(c))) {
        vanityPrefix = "";
        break;
      }
    }
    String prefix = vanityPrefix;

    List<Thread> threads = new ArrayList<>();
    KeyPairHolder keyPairHolder = new KeyPairHolder();
    for(int i=0; i<Runtime.getRuntime().availableProcessors()/2; i++) {
      Thread t = new Thread(()->{
        KeyPair keyPair;
        while(keyPairHolder.keyPair==null) {
          keyPair = KeyPair.gen();
          if(keyPair.b32Dest.startsWith(prefix)) {
            char c = keyPair.b32Dest.charAt(prefix.length());
            // require that a digit follows the prefix, so that the prefix is more distinctive
            if((c>='0' && c<='9')) {
              keyPairHolder.keyPair = keyPair;
            }
          }
        }
      });
      threads.add(t);
      t.start();
    }
    while(threads.stream().anyMatch(Thread::isAlive)) {
      try { Thread.sleep(50); } catch (InterruptedException e) {}
    }
    return keyPairHolder.keyPair;
  }

  @Override
  public void run() {

    tunnelList.load();
    for(var t : tunnelList.tunnels) if(t.getEnabled()) t.start();

    try {
      controlServerSocket = new ServerSocket(TUNNEL_CONTROL_LISTENING_PORT, 0, InetAddress.getLoopbackAddress());
      while (!stopping) {
        try (var socket = controlServerSocket.accept()) {
          var out = new PrintWriter(socket.getOutputStream(), true);
          var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

          var args = in.readLine().split(" ");

          String vanityPrefix = null;

          switch(args[0]) {

            case "server.create.vanity" : {
              if(args.length>=5) vanityPrefix = args[4];
              else if(args.length==4) vanityPrefix = args[3];
            }

            case "server.create": {
              String destHost = args[1];
              int destPort = Integer.parseInt(args[2]);
              File serverTunnelConfigDir = null;
              if(args.length>=4) serverTunnelConfigDir = new File(args[3]);
              File serverKeyFile;
              KeyPair keyPair;
              if(serverTunnelConfigDir!=null) {
                if (!serverTunnelConfigDir.exists() || serverTunnelConfigDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".keys")).length == 0) {
                  serverTunnelConfigDir.mkdir();
                  keyPair = KeyPair.gen();

                  // generate vanity address for specified 3 character prefix
                  if(vanityPrefix!=null) keyPair = generateVanityKeypair(vanityPrefix);

                  serverKeyFile = new File(serverTunnelConfigDir, keyPair.b32Dest + ".keys");
                  keyPair.write(serverKeyFile.getPath());
                } else {
                  serverKeyFile = serverTunnelConfigDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".keys"))[0];
                  keyPair = KeyPair.read(serverKeyFile.getPath());
                }
              }
              else {
                keyPair = KeyPair.gen();
              }
              var tunnel = new ServerTunnel(destHost, destPort, keyPair, getTunnelControlTempDir(), routerWrapper);
              tunnel.start();
              tunnelList.addTunnel(tunnel);
              out.println(tunnel.dest);
              break;
            }

            case "server.destroy": {
              String dest = args[1];
              tunnelList.getTunnelsCopyStream().filter(t -> t.getType().equals("server") && ((ServerTunnel) t).dest.equals(dest)).forEach(t -> {
                t.destroy(false);
                tunnelList.removeTunnel(t);
              });
              out.println("OK");
              break;
            }

            case "server.state": {
              String dest = args[1];
              tunnelList.getTunnelsCopyStream().filter(t -> t.getType().equals("server") && ((ServerTunnel) t).dest.equals(dest)).forEach(t -> {
                out.println((t.getState()));
              });
              break;
            }

            case "client.create": {
              String dest = args[1];
              int port = Integer.parseInt(args[2]);

              if(isPortAlreadyAssigned(port)) {
                out.println("ERROR - PORT ALREADY ASSIGNED");
              }
              else {
                var clientTunnel = new ClientTunnel(dest, port, routerWrapper);
                clientTunnel.start();
                tunnelList.addTunnel(clientTunnel);
                out.println(clientTunnel.port);
              }
              break;
            }

            case "client.destroy": {
              int port = Integer.parseInt(args[1]);
              tunnelList.getTunnelsCopyStream().filter(t->t.getType().equals("client") && ((ClientTunnel) t).port == port).forEach(t->{
                t.destroy(false);
                tunnelList.removeTunnel(t);
              });
              out.println("OK");
              break;
            }

            case "client.state": {
              int port = Integer.parseInt(args[1]);
              tunnelList.getTunnelsCopyStream().filter(t->t.getType().equals("client") && ((ClientTunnel) t).port == port).forEach(t->{
                out.println((t.getState()));
              });
              break;
            }

            case "socks.create": {
              int port = Integer.parseInt(args[1]);
              if(isPortAlreadyAssigned(port)) {
                out.println("ERROR - PORT ALREADY ASSIGNED");
              }
              else {
                tunnelList.addTunnel(new SocksTunnel(port, routerWrapper).start());
                out.println("OK");
              }
              break;
            }

            case "socks.destroy": {
              int port = Integer.parseInt(args[1]);
              tunnelList.getTunnelsCopyStream().filter(t -> t.getType().equals("socks") && ((SocksTunnel) t).port == port).forEach(t -> {
                t.destroy(false);
                tunnelList.removeTunnel(t);
              });
              out.println("OK");
              break;
            }
            case "socks.state": {
              int port = Integer.parseInt(args[1]);
              tunnelList.getTunnelsCopyStream().filter(t -> t.getType().equals("socks") && ((SocksTunnel) t).port == port).forEach(t -> {
                out.println((t.getState()));
              });
              break;
            }

            case "http.create": {
              int port = Integer.parseInt(args[1]);

              if(isPortAlreadyAssigned(port)) {
                out.println("ERROR - PORT ALREADY ASSIGNED");
              }
              else {
                tunnelList.addTunnel(new HttpClientTunnel(port, routerWrapper).start());
                out.println("OK");
              }

              break;
            }

            case "http.destroy": {
              int port = Integer.parseInt(args[1]);
              tunnelList.getTunnelsCopyStream().filter(t -> t.getType().equals("http") && ((SocksTunnel) t).port == port).forEach(t -> {
                t.destroy(false);
                tunnelList.removeTunnel(t);
              });
              out.println("OK");
              break;
            }
            case "http.state": {
              int port = Integer.parseInt(args[1]);
              tunnelList.getTunnelsCopyStream().filter(t -> t.getType().equals("http") && ((HttpClientTunnel) t).port == port).forEach(t -> {
                out.println((t.getState()));
              });
              break;
            }

            case "all.destroy": {
              tunnelList.getTunnelsCopyStream().forEach(t -> {
                t.destroy(false);
                tunnelList.removeTunnel(t);
              });
              out.println("OK");
              break;
            }

            case "all.list": {
              out.println(tunnelList.getJSON(false, true));
              break;
            }

            case "version": {
              out.println("i2p-zero " + UpdateCheck.currentVersion);
              break;
            }

            case "router.reachability": {
              out.println(routerWrapper.getReachability().getMessage());
              break;
            }

            case "router.isRunning": {
              out.println(routerWrapper.isRouterRunning());
              break;
            }

            case "router.externalPort": {
              out.println(routerWrapper.routerExternalPort);
              break;
            }

            case "router.setBandwidthLimitKBps": {
              int n = Integer.parseInt(args[1]);
              routerWrapper.updateBandwidthLimitKBps(n);
              out.println("OK");
              break;
            }

            case "router.getBandwidthLimitKBps": {
              out.println(routerWrapper.getBandwidthLimitKBps());
              break;
            }

            case "router.getBandwidthStats": {
              Map<String, Double> stats = new HashMap<>();
              stats.put("1sRateInKBps", routerWrapper.get1sRateInKBps());
              stats.put("1sRateOutKBps", routerWrapper.get1sRateOutKBps());
              stats.put("5mRateInKBps", routerWrapper.get5mRateInKBps());
              stats.put("5mRateOutKBps", routerWrapper.get5mRateOutKBps());
              stats.put("avgRateInKBps", routerWrapper.getAvgRateInKBps());
              stats.put("avgRateOutKBps", routerWrapper.getAvgRateOutKBps());
              stats.put("totalInMB", routerWrapper.getTotalInMB());
              stats.put("totalOutMB", routerWrapper.getTotalOutMB());
              out.println(stats.entrySet().stream().map(e->e.getKey()+"="+e.getValue()).collect(Collectors.joining(",")));
              break;
            }

            case "sam.create": {
              String[] samArgs = new String[]{"sam.keys", "127.0.0.1", "7656", "i2cp.tcp.host=127.0.0.1", "i2cp.tcp.port=7654"};
              I2PAppContext context = routerWrapper.getRouter().getContext();
              ClientAppManager mgr = new ClientAppManagerImpl(context);
              SAMBridge samBridge = new SAMBridge(context, mgr, samArgs);
              samBridge.startup();
              out.println("OK");
              break;
            }

          }

        }
        catch (Exception e) {
          if(!e.getMessage().contains("Socket closed")) e.printStackTrace();
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      if(controlServerSocket!=null) try {
        controlServerSocket.close();
      }
      catch (Exception e) { e.printStackTrace(); }
    }

  }

  public void stop(boolean fastStop) {
    stopping = true;
    try {
      getTunnelList().tunnels.forEach(t->t.destroy(fastStop));
      controlServerSocket.close();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public EepSiteTunnel getEepSiteTunnel() {
    Optional<Tunnel> eepSiteTunnelOptional = getTunnelList().getTunnelsCopyStream().filter(t->t.getType().equals("eepsite")).findFirst();
    if(eepSiteTunnelOptional.isPresent()) return (EepSiteTunnel) eepSiteTunnelOptional.get();
    else {
      EepSiteTunnel eepSiteTunnel = new EepSiteTunnel(false, KeyPair.gen(),
      System.getProperty("user.home") + File.separator + ".i2p-zero" + File.separator + "eepsite" + File.separator + "content",
      System.getProperty("user.home") + File.separator + ".i2p-zero" + File.separator + "eepsite" + File.separator + "logs",
      true, true, 8080, getTunnelControlTempDir(), routerWrapper);
      getTunnelList().addTunnel(eepSiteTunnel);
      return eepSiteTunnel;
    }
  }

  public File getTunnelControlTempDir() {
    return tunnelControlTempDir;
  }

  public static boolean isPortInUse() {
    try (var socket = new ServerSocket(TUNNEL_CONTROL_LISTENING_PORT, 0, InetAddress.getLoopbackAddress())) {
      return false;
    } catch (IOException e) {
      return true;
    }
  }

}
