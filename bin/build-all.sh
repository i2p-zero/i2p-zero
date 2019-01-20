#!/bin/bash

basedir=$(dirname $(dirname $(readlink -fm $0)))

# retrieve the I2P Java sources, OpenJDK and the Ant build tool
$basedir/bin/import-packages.sh

# build the i2p project retrieved from the I2P repository
$basedir/bin/build-original-i2p.sh

# convert the jar files from an existing I2P build into modules suitable for use with jlink
$basedir/bin/convert-jars-to-modules.sh

# compile the Java source code in this project, and then use the jlink tool
# to build a zero-dependency platform-specific launcher
$basedir/bin/build-launcher.sh
