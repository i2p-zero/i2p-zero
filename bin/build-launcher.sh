#!/bin/bash

basedir=$(dirname $(dirname $(readlink -fm $0)))

export JAVA_HOME=`realpath $basedir/import/jdk-11.0.2`

# convert the jar files from an existing I2P build into modules suitable for use with jlink
$basedir/bin/convert-jars-to-modules.sh

# compile the Main class that starts the I2P router and SAM listener
echo "*** Compiling Main class"
$JAVA_HOME/bin/javac --module-path import/lib -d target/classes $(find src -name '*.java')

# package as a modular jar
echo "*** Packaging as a modular jar"
$JAVA_HOME/bin/jar --create --file target/org.getmonero.i2p.embedded.jar --main-class org.getmonero.i2p.embedded.Main -C target/classes .

# create an OS specific launcher which will bundle together the code and a minimal JVM
echo "*** Performing jlink"
$JAVA_HOME/bin/jlink --module-path target/modules:target/org.getmonero.i2p.embedded.jar --add-modules org.getmonero.i2p.embedded --launcher router=org.getmonero.i2p.embedded --output target/router --strip-debug --compress 2 --no-header-files --no-man-pages

cp $basedir/resources/launch.sh $basedir/target/router/bin/

cp -r $basedir/import/i2p.base $basedir/target/router/
mkdir -p $basedir/target/router/i2p.config

mkdir -p $basedir/dist/
mv $basedir/target/router $basedir/dist/

echo "*** Done ***"
echo "To run, type: dist/router/bin/launch.sh"