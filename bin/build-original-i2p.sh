#!/bin/bash

basedir=$(dirname $(dirname $(readlink -fm $0)))

cd $basedir/import

OS=`uname -s`
if [ $OS = "Darwin" ]; then
    export JAVA_HOME=`realpath $basedir/import/jdks/mac/jdk-11.0.2.jdk/Contents/Home`
else
    export JAVA_HOME=`realpath $basedir/import/jdks/linux/jdk-11.0.2`
fi


# build the jars we're going to modularize
cd $basedir/import/i2p.i2p
$basedir/import/apache-ant-1.10.5/bin/ant pkg
$basedir/import/apache-ant-1.10.5/bin/ant updaterWithJbigi
cd ..


# copy the jars that we're going to modularize
mkdir -p $basedir/import/lib
for i in i2p.jar mstreaming.jar router.jar sam.jar streaming.jar; do cp $basedir/import/i2p.i2p/build/$i $basedir/import/lib/; done

# build a minimal i2p.base dir
mkdir -p $basedir/import/i2p.base
cp $basedir/import/i2p.i2p/build/jbigi.jar $basedir/import/i2p.base/
for i in blocklist.txt hosts.txt certificates; do cp -r $basedir/import/i2p.i2p/installer/resources/$i $basedir/import/i2p.base/; done

mkdir -p $basedir/import/i2p.base/geoip
for i in continents.txt countries.txt; do cp -r $basedir/import/i2p.i2p/installer/resources/$i $basedir/import/i2p.base/geoip/; done
