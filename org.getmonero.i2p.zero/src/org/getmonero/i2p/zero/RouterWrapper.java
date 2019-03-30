package org.getmonero.i2p.zero;

import net.i2p.data.router.RouterAddress;
import net.i2p.data.router.RouterInfo;
import net.i2p.router.CommSystemFacade;
import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.router.networkdb.kademlia.FloodfillNetworkDatabaseFacade;
import net.i2p.router.transport.FIFOBandwidthRefiller;
import net.i2p.router.transport.TransportUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.Properties;

public class RouterWrapper {

  private Router router;
  private boolean started = false;
  private Properties routerProperties;
  private TunnelControl tunnelControl;
  private File i2PConfigDir;
  private File i2PBaseDir;
  private Runnable updateAvailableCallback;

  public int routerExternalPort;

  public RouterWrapper(Properties routerProperties, Runnable updateAvailableCallback) {
    this.routerProperties = routerProperties;
    this.updateAvailableCallback = updateAvailableCallback;

    if(!routerProperties.contains("i2p.dir.base.template")) {
      routerProperties.put("i2p.dir.base.template", new File(new File(System.getProperty("java.home")), "i2p.base").getAbsolutePath());
    }

    int bandwidthLimitKBps = loadBandwidthLimitKBps();
    routerProperties.put("i2np.inboundKBytesPerSecond", bandwidthLimitKBps);
    routerProperties.put("i2np.outboundKBytesPerSecond", bandwidthLimitKBps);

    routerProperties.put("i2p.dir.base", System.getProperty("user.home") + File.separator + ".i2p-zero" + File.separator + "base");
    routerProperties.put("i2p.dir.config", System.getProperty("user.home") + File.separator + ".i2p-zero" + File.separator + "config");

    i2PConfigDir = new File(routerProperties.getProperty("i2p.dir.config"));
    if(!i2PConfigDir.exists()) i2PConfigDir.mkdirs();

    i2PBaseDir = new File(routerProperties.getProperty("i2p.dir.base"));
    if(!i2PBaseDir.exists()) {
      i2PBaseDir.mkdirs();
      copyFolderRecursively(Path.of(routerProperties.getProperty("i2p.dir.base.template")), i2PBaseDir.toPath());
    }
  }

  public Router getRouter() {
    return router;
  }

  public static void copyFolderRecursively(Path src, Path dest) {
    try {
      Files.walk(src).forEach(s -> {
        try {
          Path d = dest.resolve(src.relativize(s));
          if(Files.isDirectory(s)) {
            if(!Files.exists(d)) Files.createDirectory(d);
            return;
          }
          Files.copy(s, d);
        } catch(Exception e) {
          e.printStackTrace();
        }
      });
    } catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public boolean isStarted() {
    return started;
  }

  public void start(Runnable routerIsAliveCallback) {

    new Thread(()-> {
      if(started) return;
      started = true;

      router = new Router(routerProperties);

      new Thread(()->{
        router.setKillVMOnEnd(false);
        router.runRouter();
      }).start();

      new Thread(()->{
        try {

          while(true) {
            if(router.isAlive()) {
              try {
                File routerConfigFile = new File(i2PBaseDir, "router.config");
                if(!(routerConfigFile.exists() && routerConfigFile.canRead())) {
                  Thread.sleep(100);
                  continue;
                }
                Optional<String> portString = Files.lines(routerConfigFile.toPath()).filter(s -> s.startsWith("i2np.udp.port=")).findFirst();
                if(portString.isEmpty()) {
                  Thread.sleep(100);
                  continue;
                }
                routerExternalPort = Integer.parseInt(portString.get().split("=")[1]);
                break;
              }
              catch (Exception e) {
                e.printStackTrace();
              }
            }
            else {
              Thread.sleep(1000);
              System.out.println("Waiting for I2P router to start...");
            }
          }

          System.out.println("I2P router now running");

          new Thread(routerIsAliveCallback).start();

          tunnelControl = new TunnelControl(this, i2PConfigDir, new File(i2PConfigDir, "tunnelTemp"));
          new Thread(tunnelControl).start();

          UpdateCheck.scheduleUpdateCheck(i2PConfigDir, router, updateAvailableCallback);

        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }).start();

    }).start();

  }

  public void stop(boolean fastStopAndShutDown) {
    if(!started) return;
    started = false;
    tunnelControl.stop(fastStopAndShutDown);
    tunnelControl =  null;
    System.out.println("I2P router will shut down gracefully");
    router.shutdownGracefully();

    if(fastStopAndShutDown) {
      // don't wait more than 2 seconds for shutdown. If tunnels are still opening, they can pause for up to 20 seconds, which is too long
      new Thread(() -> {
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        System.exit(0);
      }).start();
    }
  }

  long lastTriggerTimestamp = 0;
  public void debouncedUpdateBandwidthLimitKBPerSec(int n) {
    long triggerTime = new Date().getTime();
    lastTriggerTimestamp = triggerTime;
    new Thread(()->{
      try { Thread.sleep(2000); } catch(InterruptedException e) {}
      if(lastTriggerTimestamp==triggerTime) {
        // nothing happened after we were triggered, so proceed
        updateBandwidthLimitKBps(n);
      }
    }).start();
  }


  public static final int defaultBandwidthKBps = (int)(0.5*1024)/8; // 0.5 MBit/s
  public int loadBandwidthLimitKBps() {
    try {
      File configFile = new File(i2PConfigDir, "config.json");
      if (configFile.exists()) {
        JSONObject root = (JSONObject) new JSONParser().parse(Files.readString(configFile.toPath()));
        return ((Long) root.get("bandwidthLimitKBps")).intValue();
      } else return defaultBandwidthKBps;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public int getBandwidthLimitKBps() {
    return Integer.parseInt(routerProperties.get("i2np.inboundKBytesPerSecond").toString());
  }

  public void updateBandwidthLimitKBps(int n) {
    routerProperties.put("i2np.inboundKBytesPerSecond", n);
    routerProperties.put("i2np.outboundKBytesPerSecond", n);

    var changes = new HashMap<String, String>();

    final int DEF_BURST_PCT = 10;
    final int DEF_BURST_TIME = 20;

    int inboundRate = n;
    int outboundRate = n;

    {
      float rate = inboundRate / 1.024f;
      float kb = DEF_BURST_TIME * rate;
      changes.put(FIFOBandwidthRefiller.PROP_INBOUND_BURST_BANDWIDTH, Integer.toString(Math.round(rate)));
      changes.put(FIFOBandwidthRefiller.PROP_INBOUND_BANDWIDTH_PEAK, Integer.toString(Math.round(kb)));
      rate -= Math.min(rate * DEF_BURST_PCT / 100, 50);
      changes.put(FIFOBandwidthRefiller.PROP_INBOUND_BANDWIDTH, Integer.toString(Math.round(rate)));
    }
    {
      float rate = outboundRate / 1.024f;
      float kb = DEF_BURST_TIME * rate;
      changes.put(FIFOBandwidthRefiller.PROP_OUTBOUND_BURST_BANDWIDTH, Integer.toString(Math.round(rate)));
      changes.put(FIFOBandwidthRefiller.PROP_OUTBOUND_BANDWIDTH_PEAK, Integer.toString(Math.round(kb)));
      rate -= Math.min(rate * DEF_BURST_PCT / 100, 50);
      changes.put(FIFOBandwidthRefiller.PROP_OUTBOUND_BANDWIDTH, Integer.toString(Math.round(rate)));
    }

    boolean saved = router.saveConfig(changes, null);
    // this has to be after the save
    router.getContext().bandwidthLimiter().reinitialize();
    if(!saved) throw new RuntimeException("Error saving the new bandwidth limit");

    try {
      File configFile = new File(i2PConfigDir, "config.json");
      JSONObject root = new JSONObject();;
      if (configFile.exists()) {
        root = (JSONObject) new JSONParser().parse(Files.readString(configFile.toPath()));
      }
      root.put("bandwidthLimitKBps", n);
      Files.writeString(configFile.toPath(), root.toJSONString());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  public double get1sRateInKBps() {
    try { return router.getContext().bandwidthLimiter().getReceiveBps()/1024d; } catch (Exception e) { return 0; }
  }
  public double get1sRateOutKBps() {
    try { return router.getContext().bandwidthLimiter().getSendBps()/1024d; } catch (Exception e) { return 0; }
  }
  public double get5mRateInKBps() {
    try {
      return router.getContext().statManager().getRate("bw.recvRate").getRate(5*60*1000).getAverageValue()/1024d;
    }
    catch (Exception e) { return 0; }
  }
  public double get5mRateOutKBps() {
    try {
      return router.getContext().statManager().getRate("bw.sendRate").getRate(5*60*1000).getAverageValue()/1024d;
    }
    catch (Exception e) { return 0; }
  }
  public double getAvgRateInKBps() {
    try {
      return router.getContext().statManager().getRate("bw.recvRate").getLifetimeAverageValue()/1024d;
    }
    catch (Exception e) { return 0; }
  }
  public double getAvgRateOutKBps() {
    try {
      return router.getContext().statManager().getRate("bw.sendRate").getLifetimeAverageValue()/1024d;
    }
    catch (Exception e) { return 0; }
  }
  public double getTotalInMB() {
    try { return router.getContext().bandwidthLimiter().getTotalAllocatedInboundBytes()/1048576d; } catch (Exception e) { return 0; }
  }
  public double getTotalOutMB() {
    try { return router.getContext().bandwidthLimiter().getTotalAllocatedOutboundBytes()/1048576d; } catch (Exception e) { return 0; }
  }

  public TunnelControl getTunnelControl() {
    return tunnelControl;
  }

  public enum NetworkState {
    HIDDEN,
    TESTING,
    FIREWALLED,
    RUNNING,
    WARN,
    ERROR,
    CLOCKSKEW,
    VMCOMM;
  }

  public static class NetworkStateMessage {
    private NetworkState state;
    private String msg;

    NetworkStateMessage(NetworkState state, String msg) {
      setMessage(state, msg);
    }

    public void setMessage(NetworkState state, String msg) {
      this.state = state;
      this.msg = msg;
    }

    public NetworkState getState() {
      return state;
    }

    public String getMessage() {
      return msg;
    }

    @Override
    public String toString() {
      return "(" + state + "; " + msg + ')';
    }
  }

  public String _t(String s) {
    return s;
  }

  final static String PROP_I2NP_NTCP_HOSTNAME = "i2np.ntcp.hostname";
  final static String PROP_I2NP_NTCP_PORT = "i2np.ntcp.port";

  public NetworkStateMessage getReachability() {
    try {
      RouterContext _context = router.getContext();
      if (_context.commSystem().isDummy())
        return new NetworkStateMessage(NetworkState.VMCOMM, "VM Comm System");
      if (_context.router().getUptime() > 60*1000 && (!_context.router().gracefulShutdownInProgress()) &&
          !_context.clientManager().isAlive())
        return new NetworkStateMessage(NetworkState.ERROR, _t("ERR-Client Manager I2CP Error - check logs"));  // not a router problem but the user should know
      // Warn based on actual skew from peers, not update status, so if we successfully offset
      // the clock, we don't complain.
      //if (!_context.clock().getUpdatedSuccessfully())
      long skew = _context.commSystem().getFramedAveragePeerClockSkew(33);
      // Display the actual skew, not the offset
      if (Math.abs(skew) > 30*1000)
        return new NetworkStateMessage(NetworkState.CLOCKSKEW, _t("ERR-Clock Skew"));
      if (_context.router().isHidden())
        return new NetworkStateMessage(NetworkState.HIDDEN, _t("Hidden"));
      RouterInfo routerInfo = _context.router().getRouterInfo();
      if (routerInfo == null)
        return new NetworkStateMessage(NetworkState.TESTING, _t("Testing"));

      CommSystemFacade.Status status = _context.commSystem().getStatus();
      NetworkState state = NetworkState.RUNNING;
      switch (status) {
        case OK:
        case IPV4_OK_IPV6_UNKNOWN:
        case IPV4_OK_IPV6_FIREWALLED:
        case IPV4_UNKNOWN_IPV6_OK:
        case IPV4_DISABLED_IPV6_OK:
        case IPV4_SNAT_IPV6_OK:
          RouterAddress ra = routerInfo.getTargetAddress("NTCP");
          if (ra == null)
            return new NetworkStateMessage(NetworkState.RUNNING, _t(status.toStatusString()));
          byte[] ip = ra.getIP();
          if (ip == null)
            return new NetworkStateMessage(NetworkState.ERROR, _t("ERR-Unresolved TCP Address"));
          // TODO set IPv6 arg based on configuration?
          if (TransportUtil.isPubliclyRoutable(ip, true))
            return new NetworkStateMessage(NetworkState.RUNNING, _t(status.toStatusString()));
          return new NetworkStateMessage(NetworkState.ERROR, _t("ERR-Private TCP Address"));

        case IPV4_SNAT_IPV6_UNKNOWN:
        case DIFFERENT:
          return new NetworkStateMessage(NetworkState.ERROR, _t("ERR-SymmetricNAT"));

        case REJECT_UNSOLICITED:
          state = NetworkState.FIREWALLED;
        case IPV4_DISABLED_IPV6_FIREWALLED:
          if (routerInfo.getTargetAddress("NTCP") != null)
            return new NetworkStateMessage(NetworkState.WARN, _t("WARN-Firewalled with Inbound TCP Enabled"));
          // fall through...
        case IPV4_FIREWALLED_IPV6_OK:
        case IPV4_FIREWALLED_IPV6_UNKNOWN:
          if (((FloodfillNetworkDatabaseFacade)_context.netDb()).floodfillEnabled())
            return new NetworkStateMessage(NetworkState.WARN, _t("WARN-Firewalled and Floodfill"));
          //if (_context.router().getRouterInfo().getCapabilities().indexOf('O') >= 0)
          //    return new NetworkStateMessage(NetworkState.WARN, _t("WARN-Firewalled and Fast"));
          return new NetworkStateMessage(state, _t(status.toStatusString()));

        case DISCONNECTED:
          return new NetworkStateMessage(NetworkState.TESTING, _t("Disconnected - check network connection"));

        case HOSED:
          return new NetworkStateMessage(NetworkState.ERROR, _t("ERR-UDP Port In Use - Set i2np.udp.internalPort=xxxx in advanced config and restart"));

        case UNKNOWN:
          state = NetworkState.TESTING;
        case IPV4_UNKNOWN_IPV6_FIREWALLED:
        case IPV4_DISABLED_IPV6_UNKNOWN:
        default:
          ra = routerInfo.getTargetAddress("SSU");
          if (ra == null && _context.router().getUptime() > 5*60*1000) {
            if (_context.commSystem().countActivePeers() <= 0)
              return new NetworkStateMessage(NetworkState.ERROR, _t("ERR-No Active Peers, Check Network Connection and Firewall"));
            else if (_context.getProperty(PROP_I2NP_NTCP_HOSTNAME) == null ||
                _context.getProperty(PROP_I2NP_NTCP_PORT) == null)
              return new NetworkStateMessage(NetworkState.ERROR, _t("ERR-UDP Disabled and Inbound TCP host/port not set"));
            else
              return new NetworkStateMessage(NetworkState.WARN, _t("WARN-Firewalled with UDP Disabled"));
          }
          return new NetworkStateMessage(state, _t(status.toStatusString()));
      }
    }
    catch(Exception e) {
      return new NetworkStateMessage(NetworkState.WARN, "Starting...");
    }
  }



}
