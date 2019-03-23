package org.getmonero.i2p.zero;

import java.io.PrintStream;
import java.util.Properties;
import java.util.stream.Collectors;

public class Main {

  public static PrintStream consoleOut = null;

  public static void main(String[] args) {

    // since i2p redirects system output, keep a reference to the console out
    consoleOut = System.out;

    if(TunnelControl.isPortInUse()) {
      System.out.println("I2P-zero already running");
      System.exit(1);
    }

    System.out.println("I2P router launched.\n" +
        "Press Ctrl-C to gracefully shut down the router (or send the SIGINT signal to the process).");

    Properties p = new Properties();
    p.put("router.sharePercentage","80");

    // allow default properties to be added via command line args, e.g. --i2p.dir.base=/usr/share/i2p
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

    RouterWrapper routerWrapper = new RouterWrapper(p, ()->{
      Main.consoleOut.println("**** A new version of I2P-zero is available at https://github.com/i2p-zero/i2p-zero - Please keep your software up-to-date, as it will enhance your privacy and keep you safe from vulnerabilities");
    });

    routerWrapper.start(()->Main.consoleOut.println("For best performance, please open port " + routerWrapper.routerExternalPort + " on your firewall for incoming UDP and TCP connections. This port has been randomly assigned to you. For privacy reasons, please do not share this port with others."));

    Runtime.getRuntime().addShutdownHook(new Thread(()->routerWrapper.stop(true)));

  }

}