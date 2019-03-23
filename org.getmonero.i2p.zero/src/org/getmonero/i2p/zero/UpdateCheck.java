package org.getmonero.i2p.zero;

import net.i2p.client.streaming.I2PSocket;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketManagerFactory;
import net.i2p.data.Destination;
import net.i2p.router.Router;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class UpdateCheck {

  public static final String currentVersion = new Scanner(RouterWrapper.class.getResourceAsStream("VERSION")).useDelimiter("\\n").next();

  public static void scheduleUpdateCheck(File i2PConfigDir, Router router, Runnable updateAvailableCallback) {

    File updateAvailableFile = new File(i2PConfigDir, "updateAvailable");
    if(updateAvailableFile.exists()) {
      updateAvailableCallback.run();
    }
    else {

      // schedule next check randomly in the next 48 hrs. On average, this call pattern will result in a check every 24 hrs.
      CompletableFuture.delayedExecutor((long) (Math.random()*3600*24*2), TimeUnit.SECONDS).execute(() -> {
        if(lookupUpdateAvailable(router)) {
          try {
            // create an empty file to indicate an update is available, so in future we don't have to keep querying the server
            updateAvailableFile.createNewFile();
          }
          catch (Exception e) {
            e.printStackTrace();
          }
          updateAvailableCallback.run();
        }
        else {
          // only check again if no update available yet
          scheduleUpdateCheck(i2PConfigDir, router, updateAvailableCallback);
        }
      });

    }

  }

  public static boolean lookupUpdateAvailable(Router router) {
    try {
      String versionAvailable = null;
      String host = "77ypf3rahyjegncradypnyotvn6fhq7sobhwe2gs5a2hdiwehwjq.b32.i2p";
      Destination dest = router.getContext().namingService().lookup(host, null, null);
      I2PSocketManager mgr = I2PSocketManagerFactory.createManager();
      I2PSocket socket = mgr.connect(dest);
      PrintWriter pw = new PrintWriter(socket.getOutputStream());
      pw.print("GET /VERSION HTTP/1.1\r\n");
      pw.print("Host: " + host + "\r\n\r\n");
      pw.flush();
      BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      String status = br.readLine();
      if (status.equals("HTTP/1.1 200 OK")) {
        String line;
        while ((line = br.readLine()) != null) {
          if (line.equals("")) break;
        }
        versionAvailable = br.readLine();
      }
      br.close();
      if(versionAvailable==null) return false;
      else return Float.parseFloat(currentVersion)<Float.parseFloat(versionAvailable);
    }
    catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

}
