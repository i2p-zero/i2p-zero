package org.getmonero.i2p.zero;

import net.i2p.I2PAppContext;
import net.i2p.app.ClientAppManager;
import net.i2p.app.ClientAppManagerImpl;
import net.i2p.client.I2PClient;
import net.i2p.client.I2PClientFactory;
import net.i2p.data.Base64;
import net.i2p.data.Destination;
import net.i2p.i2ptunnel.I2PTunnel;
import net.i2p.router.Router;
import net.i2p.sam.SAMBridge;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

public class TunnelControl implements Runnable {

  private Router router;
  private boolean stopping = false;
  private ServerSocket controlServerSocket;
  private File tunnelControlConfigDir;
  private File tunnelControlTempDir;
  private TunnelList tunnelList;

  public static class TunnelList {
    private File tunnelControlConfigDir;
    private File tunnelControlTempDir;
    private List<Tunnel> tunnels = new ArrayList<>();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public TunnelList(File tunnelControlConfigDir, File tunnelControlTempDir) {
      this.tunnelControlConfigDir = tunnelControlConfigDir;
      this.tunnelControlTempDir = tunnelControlTempDir;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
      pcs.addPropertyChangeListener(listener);
    }
    private void fireChangeEvent() {
      pcs.firePropertyChange(new PropertyChangeEvent(this, "list", null, tunnels));
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
                tunnels.add(new ServerTunnel((String) obj.get("host"), Integer.parseInt((String) obj.get("port")), new KeyPair((String) obj.get("keypair")), tunnelControlTempDir));
                break;
              case "client":
                tunnels.add(new ClientTunnel((String) obj.get("dest"), Integer.parseInt((String) obj.get("port"))));
                break;
              case "socks":
                tunnels.add(new SocksTunnel(Integer.parseInt((String) obj.get("port"))));
                break;
            }
          });
          fireChangeEvent();
        }

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    public void save() {
      try {
        JSONObject root = new JSONObject();
        JSONArray tunnelsArray = new JSONArray();
        root.put("tunnels", tunnelsArray);
        for(Tunnel t : tunnels) {
          JSONObject entry = new JSONObject();
          entry.put("type", t.getType());
          tunnelsArray.add(entry);
          switch (t.getType()) {
            case "server":
              entry.put("host", t.getHost());
              entry.put("port", t.getPort());
              entry.put("dest", t.getI2P());
              entry.put("keypair", ((ServerTunnel) t).keyPair.toString());
              break;

            case "client":
              entry.put("dest", t.getI2P());
              entry.put("port", t.getPort());
              break;

            case "socks":
              entry.put("port", t.getPort());
              break;
          }
        }
        Files.writeString(new File(tunnelControlConfigDir, "tunnels.json").toPath(), root.toJSONString());
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public TunnelControl(Router router, File tunnelControlConfigDir, File tunnelControlTempDir) {
    this.router = router;
    tunnelControlTempDir.delete();
    tunnelControlTempDir.mkdir();

    this.tunnelControlConfigDir = tunnelControlConfigDir;
    this.tunnelControlTempDir = tunnelControlTempDir;
    Runtime.getRuntime().addShutdownHook(new Thread(()->this.tunnelControlTempDir.delete()));

    tunnelList = new TunnelList(tunnelControlConfigDir, tunnelControlTempDir);
  }

  public interface Tunnel {
    public String getType();
    public String getHost();
    public String getPort();
    public String getI2P();
    public String getState();
    public void destroy();
  }

  public static class ClientTunnel implements Tunnel {
    public String dest;
    public int port;
    public I2PTunnel tunnel;
    public ClientTunnel(String dest, int port) {
      this.dest = dest;
      this.port = port;
      new Thread(()->{
        tunnel = new I2PTunnel(new String[]{"-die", "-nocli", "-e", "config localhost 7654", "-e", "client " + port + " " + dest});
      }).start();
    }
    public void destroy() {
      new Thread(()->{
        while(tunnel==null) { try { Thread.sleep(100); } catch (InterruptedException e) {} } // wait for tunnel to be established before closing it
        tunnel.runClose(new String[]{"forced", "all"}, tunnel);
      }).start();
    }

    @Override public String getType() { return "client"; }
    @Override public String getHost() { return "localhost"; }
    @Override public String getPort() { return port+""; }
    @Override public String getI2P() { return dest; }
    @Override public String getState() { return tunnel==null ? "opening..." : "open"; }
  }
  public static class ServerTunnel implements Tunnel {
    public String dest;
    public String host;
    public int port;
    public volatile I2PTunnel tunnel;
    public KeyPair keyPair;
    public ServerTunnel(String host, int port, KeyPair keyPair, File tunnelControlTempDir) {
      try {
        this.host = host;
        this.port = port;
        this.keyPair = keyPair;
        this.dest = keyPair.b32Dest;

        String uuid = new BigInteger(128, new Random()).toString(16);
        String seckeyPath = tunnelControlTempDir.getAbsolutePath() + File.separator + "seckey." + uuid + ".dat";

        Files.write(Path.of(seckeyPath), Base64.decode(keyPair.seckey));
        new File(seckeyPath).deleteOnExit(); // clean up temporary file that was only required because new I2PTunnel() requires it to be written to disk

        // listen using the I2P server keypair, and forward incoming connections to a destination and port
        new Thread(() -> {
          tunnel = new I2PTunnel(new String[]{"-die", "-nocli", "-e", "server " + host + " " + port + " " + seckeyPath});
        }).start();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    public void destroy() {
      new Thread(()->{
        while(tunnel==null) { try { Thread.sleep(100); } catch (InterruptedException e) {} } // wait for tunnel to be established before closing it
        tunnel.runClose(new String[]{"forced", "all"}, tunnel);
      }).start();
    }
    @Override public String getType() { return "server"; }
    @Override public String getHost() { return host; }
    @Override public String getPort() { return port+""; }
    @Override public String getI2P() { return dest; }
    @Override public String getState() { return tunnel==null ? "opening..." : "open"; }

  }
  public static class SocksTunnel implements Tunnel {
    public int port;
    public I2PTunnel tunnel;
    public SocksTunnel(int port) {
      this.port = port;
      new Thread(()->{
        tunnel = new I2PTunnel(new String[]{"-die", "-nocli", "-e", "sockstunnel " + port});
      }).start();
    }
    public void destroy() {
      new Thread(()->{
        while(tunnel==null) { try { Thread.sleep(100); } catch (InterruptedException e) {} } // wait for tunnel to be established before closing it
        tunnel.runClose(new String[]{"forced", "all"}, tunnel);
      }).start();
    }
    @Override public String getType() { return "socks"; }
    @Override public String getHost() { return "localhost"; }
    @Override public String getPort() { return port+""; }
    @Override public String getI2P() { return "n/a"; }
    @Override public String getState() { return tunnel==null ? "opening..." : "open"; }
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
    public static KeyPair gen() throws Exception {
      ByteArrayOutputStream seckey = new ByteArrayOutputStream();
      ByteArrayOutputStream pubkey = new ByteArrayOutputStream();
      I2PClient client = I2PClientFactory.createClient();
      Destination d = client.createDestination(seckey);
      d.writeBytes(pubkey);
      String b32Dest = d.toBase32();
      return new KeyPair(Base64.encode(seckey.toByteArray()), Base64.encode(pubkey.toByteArray()), b32Dest);
    }
    public static KeyPair read(String path) throws Exception {
      return new KeyPair(Files.readString(Paths.get(path)));
    }
    public void write(String path) throws Exception {
      Files.writeString(Paths.get(path), toString());
    }

  }

  @Override
  public void run() {

    tunnelList.load();

    try {
      controlServerSocket = new ServerSocket(30000, 0, InetAddress.getLoopbackAddress());
      while (!stopping) {
        try (var socket = controlServerSocket.accept()) {
          var out = new PrintWriter(socket.getOutputStream(), true);
          var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

          var args = in.readLine().split(" ");

          switch(args[0]) {

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
              var tunnel = new ServerTunnel(destHost, destPort, keyPair, getTunnelControlTempDir());
              tunnelList.addTunnel(tunnel);
              out.println(tunnel.dest);
              break;
            }

            case "server.destroy": {
              String dest = args[1];
              tunnelList.getTunnelsCopyStream().filter(t -> t.getType().equals("server") && ((ServerTunnel) t).dest.equals(dest)).forEach(t -> {
                t.destroy();
                tunnelList.removeTunnel(t);
              });
              out.println("OK");
              break;
            }

            case "client.create": {
              String destPubKey = args[1];
              int port = Integer.parseInt(args[2]);
              var clientTunnel = new ClientTunnel(destPubKey, port);
              tunnelList.addTunnel(clientTunnel);
              out.println(clientTunnel.port);
              break;
            }

            case "client.destroy": {
              int port = Integer.parseInt(args[1]);
              tunnelList.getTunnelsCopyStream().filter(t->t.getType().equals("client") && ((ClientTunnel) t).port == port).forEach(t->{
                t.destroy();
                tunnelList.removeTunnel(t);
              });
              out.println("OK");
              break;
            }

            case "socks.create": {
              int port = Integer.parseInt(args[1]);
              tunnelList.addTunnel(new SocksTunnel(port));
              out.println("OK");
              break;
            }

            case "socks.destroy": {
              int port = Integer.parseInt(args[1]);
              tunnelList.getTunnelsCopyStream().filter(t -> t.getType().equals("socks") && ((SocksTunnel) t).port == port).forEach(t -> {
                t.destroy();
                tunnelList.removeTunnel(t);
              });
              out.println("OK");
              break;
            }

            case "all.destroy": {
              tunnelList.getTunnelsCopyStream().forEach(t -> {
                t.destroy();
                tunnelList.removeTunnel(t);
              });
              out.println("OK");
              break;
            }

            case "sam.create": {
              String[] samArgs = new String[]{"sam.keys", "127.0.0.1", "7656", "i2cp.tcp.host=127.0.0.1", "i2cp.tcp.port=7654"};
              I2PAppContext context = router.getContext();
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

  public void stop() {
    stopping = true;
    try {
      controlServerSocket.close();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public File getTunnelControlTempDir() {
    return tunnelControlTempDir;
  }


}
