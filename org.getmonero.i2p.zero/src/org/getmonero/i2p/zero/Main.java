package org.getmonero.i2p.zero;

import java.io.File;
import java.util.Properties;
import java.util.stream.Collectors;

public class Main {

  public static void main(String[] args) {

    System.out.println("I2P router launched. SAM listening on port 7656.\n" +
        "Press Ctrl-C to gracefully shut down the router (or send the SIGINT signal to the process).");

    Properties p = new Properties();
    // add your configuration settings, directories, etc.
    // where to find the I2P installation files
    p.put("i2p.dir.base", "/usr/share/i2p");
    // where to find the I2P data files
    p.put("i2p.dir.config", System.getProperty("user.home") + File.separator + ".i2p-zero");
    // bandwidth limits in K bytes per second
    p.put("i2np.inboundKBytesPerSecond","50");
    p.put("i2np.outboundKBytesPerSecond","50");
    p.put("router.sharePercentage","80");

    // allow default properties to be overridden via command line args, e.g. --i2p.dir.base=/usr/share/i2p
    for(var arg : args) {
      if(arg.startsWith("--")) {
        String[] s = arg.split("=");
        String argName = s[0].substring("--".length());
        String argVal = s[1];
        p.put(argName, argVal);
      }
    }

    System.out.println("Options set: "
        + p.entrySet().stream().map(e->"--"+e.getKey()+"="+e.getValue()).collect(Collectors.joining(" ")));

    new RouterWrapper(p).start();

  }

}