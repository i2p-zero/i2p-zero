<img src="https://github.com/knaccc/i2p-zero/blob/master/i2p-zero.png" align="left" width="336" height="124">

## Zero dependency, small footprint, cross-platform I2P Java Router with GUI, simple tunnel/socks controller and SAM interface

##

This project will run under Linux, and build native launchers for Linux, MacOS and Windows.
The launchers will include the I2P router, a SAM listener, simple tunnel and socks tunnel functionality and a minimal JVM.

## GUI Screenshots

<img src="https://github.com/knaccc/i2p-zero/blob/master/screenshot1.png" align="center" width="497" height="440">
<br/>
<img src="https://github.com/knaccc/i2p-zero/blob/master/screenshot2.png" align="center" width="892" height="549">
<br/>
<img src="https://github.com/knaccc/i2p-zero/blob/master/screenshot3.png" align="center" width="942" height="570">

## Footprint

The zero-dependency distribution sizes are as follows:

OS | Uncompressed size (MB) | xz Compressed size (MB)
------------ | ------------- | -------------
Linux | 49.1 | 30.7
Mac | 40.0 | 25.3
Windows | 39.9 | 27.8
Linux GUI | 73.4 | 49.0
Mac GUI | 62.2 | 44.1
Windows GUI | 61.7 | 45.1

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

## Running the GUI

To run the Linux router, type:

`dist/linux-gui/router/bin/launch-gui.sh`
or double-click the `dist/linux-gui/router/i2p-zero.desktop` file

To run the MacOS router, double-click the app located at `dist/mac-gui/router/i2p-zero.app`

For Windows, double-click on `dist/windows-gui/router/bin/launch-gui.bat`

Note that for the Windows GUI to run, you may need to install the latest <a href="https://support.microsoft.com/en-us/help/2977003/the-latest-supported-visual-c-downloads">Microsoft Visual C++ Redistributable</a>


## Running the command line version

To run the Linux router, type:

`dist/linux/router/bin/launch.sh`

To run the MacOS router, type:

`dist/mac/router/bin/launch.sh`

For Windows, run:

`dist/windows/router/bin/launch.bat`

If it launches successfully, you'll see the message:

```
I2P router launched.
Press Ctrl-C to gracefully shut down the router (or send the SIGINT signal to the process).
```

## Tunnel control

Note that it may take a short while for new tunnels to be set up.

Call the `dist/linux/router/bin/tunnel-control.sh` script as follows to create and destroy tunnels:

#### Get the router reachability status. Returns a string such as "Testing", "Firewalled", "Running", "Error"

`tunnel-control.sh router.reachability`

#### Listen for I2P connections and forward them to the specified host and port. Returns the I2P base 32 destination address for the server tunnel created.

Optionally, specify a directory for storing/reading the server key file.
If the directory doesn't exist with a file named *.b32.i2p.keys in it,
returns a newly created destination address and writes the secret key for the
new address to a file called <I2P dest addr>.keys in the specified directory. Otherwise, read the existing
secret key from that directory. The server tunnel will listen for I2P connections and forward them to the
specified host and port. Note that the base 32 I2P destination address deterministically depends on the contents of the .keys file).

`tunnel-control.sh server.create <host> <port> <(optional) directory>`

#### Check the state of a tunnel. Returns "opening" or "open"

`tunnel-control.sh server.state <base 32 I2P address>`
`tunnel-control.sh client.state <local port>`
`tunnel-control.sh http.state <local port>`
`tunnel-control.sh socks.state <local port>`


#### Close the tunnel listening for connections on the specified I2P destination address. Returns "OK".

`tunnel-control.sh server.destroy <base 32 I2P address>`


#### Create a tunnel that listens for connections on localhost on the specified port and forwards connections over I2P to the specified destination public key.

`tunnel-control.sh client.create <local port> <I2P destination public key>`


#### Close the tunnel listening for connections on the specified port. Returns "OK".

`tunnel-control.sh client.destroy <local port>`

#### Create an http proxy (for accessing .i2p web sites), listening on the specified port

`tunnel-control.sh http.create <local port>`

#### Destroy the http proxy listening on the specified port

`tunnel-control.sh http.destroy <local port>`

#### Create a socks tunnel, listening on the specified port

`tunnel-control.sh socks.create <local port>`

#### Destroy the socks tunnel listening on the specified port

`tunnel-control.sh socks.destroy <local port>`

#### Destroy all tunnels. Returns "OK"

`tunnel-control.sh all.destroy`

#### List all tunnels. Returns JSON string containing information about all tunnels currently in existence

`tunnel-control.sh all.list`

#### Start a SAM listener on port 7656. Returns "OK"

`tunnel-control.sh sam.create`


## Watch the I2P log for messages

`tail -f dist/linux/router/i2p.config/wrapper.log`