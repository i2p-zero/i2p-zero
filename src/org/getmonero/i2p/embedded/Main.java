package org.getmonero.i2p.embedded;

import java.io.File;
import java.util.Properties;

import net.i2p.I2PAppContext;
import net.i2p.app.ClientAppManager;
import net.i2p.app.ClientAppManagerImpl;
import net.i2p.router.Router;
import net.i2p.sam.SAMBridge;

public class Main {

  public static void main(String[] args) {

    System.out.println("I2P router launched.");

    Properties p = new Properties();
    // add your configuration settings, directories, etc.
    // where to find the I2P installation files
    p.put("i2p.dir.base", "/usr/share/i2p");
    // where to find the I2P data files
    p.put("i2p.dir.config", System.getProperty("user.home") + File.separator + ".i2p");
    // bandwidth limits in K bytes per second
    p.put("i2np.inboundKBytesPerSecond","50");
    p.put("i2np.outboundKBytesPerSecond","50");
    p.put("router.sharePercentage","80");

    Router r = new Router(p);

    new Thread() {
      @Override
      public void run() {
        // don't call exit() when the router stops
        r.setKillVMOnEnd(false);
        r.runRouter();
      }
    }.start();

    new Thread() {
      @Override
      public void run() {
        try {
          String[] args = new String[]{"sam.keys", "127.0.0.1", "7656", "i2cp.tcp.host=127.0.0.1", "i2cp.tcp.port=7654"};
          I2PAppContext context = r.getContext();
          ClientAppManager mgr = new ClientAppManagerImpl(context);
          SAMBridge samBridge = new SAMBridge(context, mgr, args);
          samBridge.startup();
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }.start();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        System.out.println("I2P router will shut down gracefully");
        r.shutdownGracefully();
      }
    });

  }

}