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
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class UpdateCheck {

  public static final String currentVersion = new Scanner(RouterWrapper.class.getResourceAsStream("VERSION")).useDelimiter("\\n").next();

  private static boolean isNewerVersionAvailable(String currentVersion, String versionAvailable) {
    int currentMajor = Integer.parseInt(currentVersion.split(".")[0]);
    int currentMinor = Integer.parseInt(currentVersion.split(".")[1]);
    int availableMajor = Integer.parseInt(versionAvailable.split(".")[0]);
    int availableMinor = Integer.parseInt(versionAvailable.split(".")[1]);
    if(availableMajor>currentMajor) return true;
    if(availableMajor<currentMajor) return false;
    if(availableMinor>currentMinor) return true;
    return false;
  }

  public static void scheduleUpdateCheck(File i2PConfigDir, Router router, Runnable updateAvailableCallback) {

    try {
      File versionAvailableFile = new File(i2PConfigDir, "versionAvailable");
      if(!versionAvailableFile.exists()) {
        Files.write(versionAvailableFile.toPath(), currentVersion.getBytes(), StandardOpenOption.CREATE);
      }

      String versionAvailable = Files.readString(versionAvailableFile.toPath());
      if(isNewerVersionAvailable(currentVersion, versionAvailable)) {
        updateAvailableCallback.run();
      }
      else {

        // schedule next check randomly in the next 48 hrs. On average, this call pattern will result in a check every 24 hrs.
        CompletableFuture.delayedExecutor((long) (Math.random()*3600*24*2), TimeUnit.SECONDS).execute(() -> {
          String versionAvailableLookup = lookupVersionAvailable(router);
          if(versionAvailableLookup!=null) {
            try {
              // create an empty file to indicate an update is available, so in future we don't have to keep querying the server
              Files.write(versionAvailableFile.toPath(), versionAvailableLookup.getBytes(), StandardOpenOption.CREATE);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
          scheduleUpdateCheck(i2PConfigDir, router, updateAvailableCallback);
        });

      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }

  }

  public static String lookupVersionAvailable(Router router) {
    try {
      String sanitizedVersionAvailable = null;
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
        String rawVersionAvailable = br.readLine();
        // sanitize, just in case the update notification server is compromised
        if(rawVersionAvailable.length()>10) return null;
        String allowedChars = "0123456789.";
        sanitizedVersionAvailable = "";
        for(var i=0; i<rawVersionAvailable.length(); i++) {
          if(allowedChars.indexOf(rawVersionAvailable.charAt(i))>0) sanitizedVersionAvailable += rawVersionAvailable.charAt(i);
        }
      }
      br.close();
      return sanitizedVersionAvailable;
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

}
