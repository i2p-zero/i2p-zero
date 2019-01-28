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

import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TunnelControl implements Runnable {

  private List<Tunnel> tunnels = new ArrayList<>();
  private int clientPortSeq = 30000;
  private Router router;
  private boolean stopping = false;
  private ServerSocket controlServerSocket;
  private File tunnelControlTempDir;


  public TunnelControl(Router router, File tunnelControlTempDir) {
    this.router = router;
    tunnelControlTempDir.delete();
    tunnelControlTempDir.mkdir();
    this.tunnelControlTempDir = tunnelControlTempDir;
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
    public I2PTunnel tunnel;
    public KeyPair keyPair;
    public ServerTunnel(String host, int port, KeyPair keyPair, File tunnelControlTempDir) throws Exception {
      this.host = host;
      this.port = port;
      this.keyPair = keyPair;
      this.dest = keyPair.b32Dest;

      String uuid = new BigInteger(128, new Random()).toString(16);
      String seckeyPath = tunnelControlTempDir.getAbsolutePath() + File.separator + "seckey."+uuid+".dat";

      Files.write(Path.of(seckeyPath), Base64.decode(keyPair.seckey));

      // listen using the I2P server keypair, and forward incoming connections to a destination and port
      new Thread(()->{
        tunnel = new I2PTunnel(new String[]{"-die", "-nocli", "-e", "server "+host+" "+port+" " + seckeyPath});
      }).start();
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

  public List<Tunnel> getTunnels() {
    return tunnels;
  }

  public KeyPair genKeyPair() throws Exception {
    ByteArrayOutputStream seckey = new ByteArrayOutputStream();
    ByteArrayOutputStream pubkey = new ByteArrayOutputStream();
    I2PClient client = I2PClientFactory.createClient();
    Destination d = client.createDestination(seckey);
    d.writeBytes(pubkey);
    String b32Dest = d.toBase32();
    return new KeyPair(Base64.encode(seckey.toByteArray()), Base64.encode(pubkey.toByteArray()), b32Dest);
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
    public KeyPair(String base64EncodedCommaDelimitedPair) throws Exception {
      String[] a = base64EncodedCommaDelimitedPair.split(",");
      this.seckey = a[0];
      this.pubkey = a[1];
      Destination d = new Destination();
      d.readBytes(new ByteArrayInputStream(Base64.decode(this.seckey)));
      this.b32Dest = d.toBase32();
    }
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

    try {
      controlServerSocket = new ServerSocket(clientPortSeq++, 0, InetAddress.getLoopbackAddress());
      while (!stopping) {
        try (var socket = controlServerSocket.accept()) {
          var out = new PrintWriter(socket.getOutputStream(), true);
          var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

          var args = in.readLine().split(" ");

          switch(args[0]) {

            case "server.create":
              String destHost = args[1];
              int destPort = Integer.parseInt(args[2]);
              var tunnel = new ServerTunnel(destHost, destPort, genKeyPair(), getTunnelControlTempDir());
              tunnels.add(tunnel);
              out.println(tunnel.dest);
              break;

            case "server.destroy":
              String dest = args[1];
              new ArrayList<>(tunnels).stream().filter(t->t.getType().equals("server") && ((ServerTunnel) t).dest.equals(dest)).forEach(t->{
                t.destroy();
                tunnels.remove(t);
              });
              out.println("OK");
              break;

            case "client.create":
              String destPubKey = args[1];
              var clientTunnel = new ClientTunnel(destPubKey, clientPortSeq++);
              tunnels.add(clientTunnel);
              out.println(clientTunnel.port);
              break;

            case "client.destroy": {
              int port = Integer.parseInt(args[1]);
              new ArrayList<>(tunnels).stream().filter(t->t.getType().equals("client") && ((ClientTunnel) t).port == port).forEach(t->{
                t.destroy();
                tunnels.remove(t);
              });
              out.println("OK");
              break;
            }

            case "socks.create": {
              int port = Integer.parseInt(args[1]);
              tunnels.add(new SocksTunnel(port));
              out.println("OK");
              break;
            }

            case "socks.destroy":
              int port = Integer.parseInt(args[1]);
              new ArrayList<>(tunnels).stream().filter(t->t.getType().equals("socks") && ((SocksTunnel) t).port == port).forEach(t->{
                t.destroy();
                tunnels.remove(t);
              });
              out.println("OK");
              break;

            case "sam.create":
              String[] samArgs = new String[]{"sam.keys", "127.0.0.1", "7656", "i2cp.tcp.host=127.0.0.1", "i2cp.tcp.port=7654"};
              I2PAppContext context = router.getContext();
              ClientAppManager mgr = new ClientAppManagerImpl(context);
              SAMBridge samBridge = new SAMBridge(context, mgr, samArgs);
              samBridge.startup();
              out.println("OK");
              break;

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
