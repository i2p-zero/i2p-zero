# Embedded I2P Java Router with SAM interface

This project will build a native launcher. The launcher will include the I2P router, a SAM listener and a minimal JVM.

## Prerequisites

This project requires the JAR files and base configuration dir from an existing I2P installation.

To install I2P on Ubuntu so that these files are available to you, type:

```
sudo apt-add-repository ppa:i2p-maintainers/i2p
sudo apt-get update
sudo apt-get install i2p
```

Then copy the following 56 JAR files from the I2P installation to the import/lib directory in your clone of this GitHub project:

```
mkdir -p import/lib
for i in i2p.jar mstreaming.jar router.jar sam.jar streaming.jar gnu-getopt.jar libintl.jar; do cp /usr/share/i2p/lib/$i import/lib/; done
```

You will need OpenJDK 11 installed:

`sudo apt install openjdk-11-jdk-headless`

## Building the launcher

`bin/build-launcher.sh`

This will convert the imported JARs to modules, compile the Java source code in this project, and then use the jlink tool
to build a platform-specific launcher executable.

## Running the launcher

The launcher will need access to the /usr/share/i2p base directory. It will create a .i2p directory for configuration
files in the current user's home directory if it does not already exist.

`target/router/bin/router`

## Check that the I2P router is running and that it is listening for SAM connections

`fuser 7656/tcp`

## Todo
Need to determine the most minimal base configuration directory contents, and include it as part of this project.
