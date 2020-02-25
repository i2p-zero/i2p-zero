<img src="https://github.com/i2p-zero/i2p-zero/blob/master/i2p-zero.png" align="left" width="336" height="124">

## Zero dependency, small footprint, cross-platform I2P Java Router with GUI, simple tunnel/socks controller and SAM interface

##

Note that I2P-zero is not a re-implementation of I2P. It uses the exact I2P source code from the official I2P GitHub repository.

I2P-zero is a build script that produces a zero-dependency installation of the official I2P release, and includes a simplified front end interface.

This project will run under Linux, and build native launchers for Linux, MacOS and Windows.
The launchers will include the I2P router, a SAM listener, simple tunnel and socks tunnel functionality and a minimal JVM.

## Downloads

Download the latest binary releases for Mac/Windows/Linux here: https://github.com/i2p-zero/i2p-zero/releases

## GUI Screenshots

<img src="https://github.com/i2p-zero/i2p-zero/blob/master/screenshot1.png" align="center" width="472" height="440">
<br/>
<img src="https://github.com/i2p-zero/i2p-zero/blob/master/screenshot2.png" align="center" width="892" height="549">
<br/>
<img src="https://github.com/i2p-zero/i2p-zero/blob/master/screenshot3.png" align="center" width="880" height="540">

## Footprint

The zero-dependency distribution sizes are as follows:

OS           | Uncompressed size (MB)  | Compressed size (MB)  | v1.15 Reproducible build SHA-256
------------ | ----------------------- | --------------------- | ------------------------------------------------------------------
Mac          | 40.0                    | 26.7                  | `bd483be3f3528afda4620ae05ce4be0528993388e913148f95af36496e6525b0`
Windows      | 41.9                    | 30.5                  | `cb6839c759a0a56dd0a6614de9576add4fc784a617c4a8677aab953b721e970c`
Linux        | 51.6                    | 33.3                  | `d41f79bfa9dadfa7050736fd7e1a77a623bd8db966742cb4c2a642150702621b`
Mac GUI      | 62.9                    | 45.8                  | `540952c4b6c61455d9efa88a1033b1f5d68edc5db8c81ed1b99e485b3b0d4adc`
Windows GUI  | 63.8                    | 48.8                  | `92bc613047b9059e5673fbb54bb28eda590abe53c26ce48869273a6d81731450`
Linux GUI    | 77.6                    | 53.8                  | `31f0327401f93fc4ebe608db9e46c354e93f41c457c4e1151ad4a28b5f992bfe`

Note: Reproducible builds are currently experimental. Due to JDK differences, Builds on Mac will consistently have different hashes than builds on Linux. Official releases will always be built on Linux (Ubuntu).


## Building the launchers

All binary releases for Windows, Mac and Linux can be built from either Linux or Mac.

From a freshly installed Ubuntu system, first ensure git is installed:

`sudo apt install git`

Then, retrieve this project from git:

`git clone https://github.com/i2p-zero/i2p-zero.git`

Note that the current version of this script uses jdk-13. If this version of Java becomes no longer available for
download, then update the references in java-config.sh to the later version. To locate a recent
JDK download URL, see https://jdk.java.net/13/

Also note that JDKs for Linux, MacOS and Windows will be downloaded, which will total several hundred megabytes. You may need to ensure your system has zip, unzip and bzip2 installed to run the build script.

Run the `bin/build-all-and-zip.sh` script, which will in turn call the following scripts:

1. `bin/import-packages.sh` to retrieve the I2P Java sources, OpenJDK and the Ant build tool

2. `bin/build-original-i2p.sh` to build the I2P project retrieved from the I2P repository

3. `bin/build-launcher.sh` to convert the I2P JARs to modules, compile the Java source code in this project, and then use
the jlink tool to build zero-dependency platform-specific launchers.

4. `bin/zip-all.sh` to produce the distribution zip files and display their SHA-256 hashes. Note that reproducible builds are currently a work in progress, and that only builds on Linux will show the same hashes as the official releases.

## Running the GUI

To run the Linux router, double-click the app located at `dist/linux-gui/router/i2p-zero`

To run the MacOS router, double-click the app located at `dist/mac-gui/router/i2p-zero.app`

For Windows, double-click the app located at `dist/windows-gui/router/i2p-zero.exe`

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

#### Find out if the router is running (where "running" means it has warmed up and is allowing I2P connections to be created). Returns "true" or "false"

`tunnel-control.sh router.isRunning`

#### Listen for I2P connections and forward them to the specified host and port. Returns the I2P base 32 destination address for the server tunnel created.

Optionally, specify a directory for storing/reading the server key file.
If the directory doesn't exist with a file named *.b32.i2p.keys in it,
returns a newly created destination address and writes the secret key for the
new address to a file called <I2P dest addr>.keys in the specified directory. Otherwise, read the existing
secret key from that directory. The server tunnel will listen for I2P connections and forward them to the
specified host and port. Note that the base 32 I2P destination address deterministically depends on the contents of the .keys file).

`tunnel-control.sh server.create <host> <port> <(optional) directory>`

or, if you would like a vanity b32 address for your server tunnel that begins with a 3 character (alphanumeric) prefix, type:

`tunnel-control.sh server.create.vanity <host> <port> <directory> <prefix>`

If you do not want to specify the directory parameter above, specify `none` as the directory. Note that this command may take several minutes to complete.

#### Check the state of a tunnel. Returns "opening" or "open"

`tunnel-control.sh server.state <base 32 I2P address>`
`tunnel-control.sh client.state <local port>`
`tunnel-control.sh http.state <local port>`
`tunnel-control.sh socks.state <local port>`


#### Close the tunnel listening for connections on the specified I2P destination address. Returns "OK".

`tunnel-control.sh server.destroy <base 32 I2P address>`


#### Create a tunnel that listens for connections on localhost on the specified port and forwards connections over I2P to the specified destination public key.

`tunnel-control.sh client.create <I2P destination> <local port>`


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

#### Get the external port randomly assigned to this router when first run, which the firewall should allow incoming UDP and TCP connections on. Returns the port number.

`tunnel-control.sh router.externalPort`

#### Set the bandwidth limit, measured in KBps. Returns "OK".

`tunnel-control.sh router.setBandwidthLimitKBps <KBps>`

#### Get the bandwidth limit, measured in KBps

`tunnel-control.sh router.getBandwidthLimitKBps`

#### Get bandwidth statistics. Returns a comma separated list of statistics

`tunnel-control.sh router.getBandwidthStats`

example response:

`1sRateInKBps=12.34,1sRateOutKBps=12.34,5mRateInKBps=12.34,5mRateOutKBps=12.34,avgRateInKBps=12.34,avgRateOutKBps=12.34,totalInMB=12.34,totalOutMB=12.34`

or, for pleasant viewing on the command line, automatically updating every 2 seconds:

`watch "tunnel-control.sh router.getBandwidthStats | tr ',' '\n' | sort"`

#### Get the I2P-zero version

`tunnel-control.sh version`

example response:

`i2p-zero 1.8`


## Watch the I2P log for messages

`tail -f dist/linux/router/i2p.config/wrapper.log`


## Note on bundled windows wrapper.exe executable

There is a bundled resources/wrapper.exe file in the source tree. This allows the windows distributable to be built
even on a non-windows platform.

This file can be deterministically recreated by
downloading javapackager from http://download2.gluonhq.com/jpackager/11/jdk.packager-windows.zip, unzipping it,
unzipping the jar file within that, and obtaining the jdk/packager/internal/resources/windows/papplauncher.exe file.

Then, on Windows, download Resource Hacker version 5.1.7 from http://www.angusj.com/resourcehacker/ and use it to add the
resources/icons.ico file to the papplauncher.exe file. Finally, verify that the resulting file is identical to the bundled resources/wrapper.exe file.

The sha256 checksum of the file should be `50c9286b9da7a91b8715de3cbcd141ec44eb199642562f43ce82351609115e06`