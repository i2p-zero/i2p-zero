<img src="https://github.com/knaccc/i2p-zero/blob/master/i2p-zero.png" align="left" width="336" height="124">

## Zero dependency, small footprint, cross-platform I2P Java Router with simple tunnel/socks controller and SAM interface

##

This project will run under Linux, and build native launchers for Linux, MacOS and Windows.
The launchers will include the I2P router, a SAM listener, simple tunnel and socks tunnel functionality and a minimal JVM.

## Footprint

The zero-dependency distribution sizes are as follows:

OS | Uncompressed size (MB) | xz Compressed size (MB)
------------ | ------------- | -------------
Linux | 42.6 | 23.4
MacOS | 40.2 | 21.1
Windows | 33.1 | 20.8

## Building the launchers

From a freshly installed Ubuntu system, first ensure git is installed:

`sudo apt install git`

Then, retrieve this project from git:

`git clone https://github.com/knaccc/i2p-zero.git`

Note that the current version of this script uses jdk-11.0.2. If this version of Java becomes no longer available for
download, then update the references to jdk-11.0.2 in java-config.sh to the later version. To locate a recent
JDK download URL, see https://jdk.java.net/11/

Also note that JDKs for Linux, MacOS and Windows will be downloaded, which will total several hundred megabytes.

Run the `bin/build-all.sh` script, which will in turn call the following scripts:

1. `bin/import-packages.sh` to retrieve the I2P Java sources, OpenJDK and the Ant build tool

2. `bin/build-original-i2p.sh` to build the I2P project retrieved from the I2P repository

3. `build-launcher.sh` to convert the I2P JARs to modules, compile the Java source code in this project, and then use
the jlink tool to build zero-dependency platform-specific launchers.

## Running the launchers

To run the Linux router, type:

`dist/linux/router/bin/launch.sh`

To run the MacOS router, type:

`dist/mac/router/bin/launch.sh`

For Windows, run:

`dist/windows/router/bin/launch.bat`

If it launches successfully, you'll see the message:

```
I2P router launched. SAM listening on port 7656.
Press Ctrl-C to gracefully shut down the router (or send the SIGINT signal to the process).
```

## Check that the I2P router is running and that it is listening for SAM connections

`fuser 7656/tcp`


## Tunnel control

Call the `dist/linux/router/bin/tunnel-control.sh` script as follows to create and destroy tunnels:

#### Listen for i2p connections and forward them to the specified host and port. Returns a newly created destination public key.

`tunnel-control.sh server.create <host> <port>`


#### Close the tunnel listening for connections on the specified destination public key. Returns "OK".

`tunnel-control.sh server.destroy <i2p destination public key>`


#### Create a tunnel that listens for connections on localhost and forwards connections over I2P to the specified destination public key. Returns a newly created localhost port number.

`tunnel-control.sh client.create <i2p destination public key>`


#### Close the tunnel listening for connections on the specified port. Returns "OK".

`tunnel-control.sh client.destroy <port>`

#### Create a socks tunnel, listening on the specified port

`tunnel-control.sh socks.create <port>`

#### Destroy the socks tunnel listening on the specified port

`tunnel-control.sh socks.destroy <port>`

#### Start a SAM listener on port 7656. Returns "OK"

`tunnel-control.sh sam.create`


## Watch the I2P log for messages

`tail -f dist/linux/router/i2p.config/wrapper.log`
