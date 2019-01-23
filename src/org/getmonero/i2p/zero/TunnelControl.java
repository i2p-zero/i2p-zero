package org.getmonero.i2p.zero;

import net.i2p.I2PAppContext;
import net.i2p.app.ClientAppManager;
import net.i2p.app.ClientAppManagerImpl;
import net.i2p.data.Base64;
import net.i2p.data.Destination;
import net.i2p.i2ptunnel.I2PTunnel;
import net.i2p.router.Router;
import net.i2p.sam.SAMBridge;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class TunnelControl implements Runnable {

  private Map<Integer, I2PTunnel> clientTunnels = new HashMap<>();
  private Map<String, I2PTunnel> serverTunnels = new HashMap<>();
  private Map<Integer, I2PTunnel> socksTunnels = new HashMap<>();
  private int clientPortSeq = 30000;
  private int serverSeq = 0;
  private String tunnelConfigDirPrefix;
  private Router router;
  public TunnelControl(Router router, File tunnelConfigDir) {
    this.router = router;
    tunnelConfigDir.delete();
    tunnelConfigDir.mkdir();
    this.tunnelConfigDirPrefix = tunnelConfigDir.getAbsolutePath() + File.separator;
  }

  @Override
  public void run() {

    // listen for socket connections to the tunnel controller.
    // listen for the commands on port 30000:
    // server.create <host> <port> // returns a newly created destination public key, which will listen for i2p connections and forward them to the specified host and port
    // server.destroy <i2p destination public key> // closes the tunnel listening for connections on the specified destination public key, and returns OK
    // client.create <i2p destination public key> // returns a newly created localhost port number, where connections will be sent over I2P to the destination public key
    // client.destroy <port> // closes the tunnel listening for connections on the specified port, and returns OK
    // socks.create <port> // creates a socks proxy listening on the specified port
    // socks.destroy <port> // closes the socks proxy listening on the specified port, and returns OK
    //
    // send a command with bash: exec 3<>/dev/tcp/localhost/30000; echo "server.create localhost 80" >&3; cat <&3
    //
    try (var listener = new ServerSocket(clientPortSeq++, 0, InetAddress.getLoopbackAddress())) {
      while (true) {
        try (var socket = listener.accept()) {
          var out = new PrintWriter(socket.getOutputStream(), true);
          var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

          var args = in.readLine().split(" ");

          if(args[0].equals("server.create")) {
            String destHost = args[1];
            String destPort = args[2];
            String seckeyPath = tunnelConfigDirPrefix + "seckey."+serverSeq+".dat";
            String pubkeyPath = tunnelConfigDirPrefix + "pubkey."+serverSeq+".dat";
            serverSeq++;
            new I2PTunnel(new String[]{"-die", "-nocli", "-e", "genkeys " + seckeyPath + " " + pubkeyPath});
            String destPubKey = Base64.encode(Files.readAllBytes(new File(pubkeyPath).toPath()));

            // listen using the I2P server keypair, and forward incoming connections to a destination and port
            new Thread(()->{
              I2PTunnel t = new I2PTunnel(new String[]{"-die", "-nocli", "-e", "server "+destHost+" "+destPort+" " + seckeyPath});
              serverTunnels.put(destPubKey, t);
            }).start();
            try (var fis = new FileInputStream(seckeyPath)) {
              Destination d = new Destination();
              d.readBytes(fis);
              out.println(d.toBase32());
            }
          }
          else if(args[0].equals("server.destroy")) {
            String destPubKey = args[1];
            new Thread(()->{
              var t = serverTunnels.get(destPubKey);
              serverTunnels.remove(destPubKey);
              t.runClose(new String[]{"forced", "all"}, t);
            }).start();
            out.println("OK");
          }
          else if(args[0].equals("client.create")) {
            String destPubKey = args[1];
            int port = clientPortSeq++;
            new Thread(()->{
              var t = new I2PTunnel(new String[]{"-die", "-nocli", "-e", "config localhost 7654", "-e", "client " + port + " " + destPubKey});
              clientTunnels.put(port, t);
            }).start();
            out.println(port);
          }
          else if(args[0].equals("client.destroy")) {
            int port = Integer.parseInt(args[1]);
            new Thread(()->{
              var t = clientTunnels.get(port);
              clientTunnels.remove(port);
              t.runClose(new String[]{"forced", "all"}, t);
            }).start();
            out.println("OK");
          }
          else if(args[0].equals("socks.create")) {
            int port = Integer.parseInt(args[1]);
            new Thread(()->{
              var t = new I2PTunnel(new String[]{"-die", "-nocli", "-e", "sockstunnel " + port});
              socksTunnels.put(port, t);
            }).start();
            out.println("OK");
          }
          else if(args[0].equals("socks.destroy")) {
            int port = Integer.parseInt(args[1]);
            new Thread(()->{
              var t = socksTunnels.get(port);
              socksTunnels.remove(port);
              t.runClose(new String[]{"forced", "all"}, t);
            }).start();
            out.println("OK");
          }
          else if(args[0].equals("sam.create")) {
            String[] samArgs = new String[]{"sam.keys", "127.0.0.1", "7656", "i2cp.tcp.host=127.0.0.1", "i2cp.tcp.port=7654"};
            I2PAppContext context = router.getContext();
            ClientAppManager mgr = new ClientAppManagerImpl(context);
            SAMBridge samBridge = new SAMBridge(context, mgr, samArgs);
            samBridge.startup();
            out.println("OK");
          }

        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }

  }
}
