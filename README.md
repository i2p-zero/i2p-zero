# Embedded I2P Java Router with SAM interface

This project will build a native launcher. The launcher will include the I2P router, a SAM listener and a minimal JVM.

## Building the launcher

First, retrieve this project from git:

`git clone https://github.com/knaccc/embedded-i2p-java-router-with-sam.git`

Note that the current version of this script uses jdk-11.0.2. If this version of Java becomes no longer available for
download, then update the references to jdk-11.0.2 in this folder structure to the later version. To locate a recent
JDK download URL, see https://jdk.java.net/11/

Run the `bin/build-all.sh` script, which will in turn call the following scripts:

1. `bin/import-packages.sh` to retrieve the I2P Java sources, OpenJDK and the Ant build tool

2. `bin/build-original-i2p.sh` to build the I2P project retrieved from the I2P repository

3. `build-launcher.sh` to convert the I2P JARs to modules, compile the Java source code in this project, and then use
the jlink tool to build a zero-dependency platform-specific launcher.

## Running the launcher

To run the router, type:

`target/router/bin/launch.sh`

If it launches successfully, you'll see the message:

```
I2P router launched. SAM listening on port 7656.
Press Ctrl-C to gracefully shut down the router (or send the SIGINT signal to the process).
```

## Check that the I2P router is running and that it is listening for SAM connections

`fuser 7656/tcp`
