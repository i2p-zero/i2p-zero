package org.getmonero.i2p.zero;

import net.i2p.router.Router;
import net.i2p.router.transport.FIFOBandwidthRefiller;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

public class RouterWrapper {

  private Router router;
  private boolean started = false;
  private Properties routerProperties;
  private TunnelControl tunnelControl;

  public RouterWrapper(Properties p) {
    this.routerProperties = p;
  }

  public boolean isStarted() {
    return started;
  }

  public void start() {

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
              break;
            }
            else {
              Thread.sleep(1000);
              System.out.println("Waiting for I2P router to start...");
            }
          }

          tunnelControl = new TunnelControl(router, new File(new File(routerProperties.getProperty("i2p.dir.config")), "tunnel"));
          new Thread(tunnelControl).start();

        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }).start();

      Runtime.getRuntime().addShutdownHook(new Thread(()->stop()));

    }).start();

  }

  public void stop() {
    if(!started) return;
    started = false;
    tunnelControl.stop();
    System.out.println("I2P router will shut down gracefully");
    router.shutdownGracefully();
  }

  long lastTriggerTimestamp = 0;
  public void debouncedUpdateBandwidthLimitKBPerSec(int n) {
    long triggerTime = new Date().getTime();
    lastTriggerTimestamp = triggerTime;
    new Thread(()->{
      try { Thread.sleep(2000); } catch(InterruptedException e) {}
      if(lastTriggerTimestamp==triggerTime) {
        // nothing happened after we were triggered, so proceed
        updateBandwidthLimitKBPerSec(n);
      }
    }).start();
  }

  public void updateBandwidthLimitKBPerSec(int n) {
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


}
