#!/bin/bash

basedir=$(dirname $(dirname $(readlink -fm $0)))

# retrieve the I2P Java sources, OpenJDK and the Ant build tool
$basedir/bin/import-packages.sh

# build the i2p project retrieved from the I2P repository
$basedir/bin/build-original-i2p.sh

# convert the imported JARs to modules, compile the Java source code in this project, and then use the jlink tool
# to build a zero-dependency platform-specific launcher
$basedir/bin/build-launcher.sh
