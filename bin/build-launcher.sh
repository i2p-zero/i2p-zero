#!/bin/bash

basedir=$(dirname $(dirname $(readlink -fm $0)))

source $basedir/bin/java-config.sh

# compile the Main class that starts the I2P router and SAM listener
echo "*** Compiling Main class"
$JAVA_HOME/bin/javac --module-path import/lib -d target/classes $(find src -name '*.java')

# package as a modular jar
echo "*** Packaging as a modular jar"
$JAVA_HOME/bin/jar --create --file target/org.getmonero.i2p.zero.jar --main-class org.getmonero.i2p.zero.Main -C target/classes .

rm -fr $basedir/dist
mkdir -p $basedir/dist/linux $basedir/dist/mac $basedir/dist/win

# create OS specific launchers which will bundle together the code and a minimal JVM
echo "*** Performing jlink (Linux)"
$JAVA_HOME/bin/jlink --module-path ${JAVA_HOME_LINUX}/jmods:target/modules:target/org.getmonero.i2p.zero.jar --add-modules org.getmonero.i2p.zero --output dist/linux/router --strip-debug --compress 2 --no-header-files --no-man-pages

echo "*** Performing jlink (Mac)"
$JAVA_HOME/bin/jlink --module-path ${JAVA_HOME_MAC}/Contents/Home/jmods:target/modules:target/org.getmonero.i2p.zero.jar --add-modules org.getmonero.i2p.zero --output dist/mac/router --strip-debug --compress 2 --no-header-files --no-man-pages

echo "*** Performing jlink (Windows)"
$JAVA_HOME/bin/jlink --module-path ${JAVA_HOME_WIN}/jmods:target/modules:target/org.getmonero.i2p.zero.jar --add-modules org.getmonero.i2p.zero --output dist/win/router --strip-debug --compress 2 --no-header-files --no-man-pages


for i in linux mac; do
  cp $basedir/resources/launch.sh $basedir/dist/$i/router/bin/
  cp $basedir/resources/tunnel-control.sh $basedir/dist/$i/router/bin/
done
cp $basedir/resources/launch.bat $basedir/dist/win/router/bin/

for i in linux mac win; do cp -r $basedir/import/i2p.base $basedir/dist/$i/router/; done
for i in linux mac win; do mkdir -p $basedir/dist/$i/router/i2p.config; done

# remove unnecessary native libs from jbigi.jar
for i in linux mac win; do
  for j in freebsd linux mac win; do
    if [ "$i" != "$j" ]; then
      if [ "$j" = "mac" ]; then j="osx"; fi
      if [ "$j" = "win" ]; then j="windows"; fi
      zip -d $basedir/dist/$i/router/i2p.base/jbigi.jar *-${j}-*
    fi
  done
done

du -sk dist/* | awk '{printf "%.1f MB %s\n",$1/1024,$2}'

echo "*** Done ***"
echo "To run, type: dist/linux/router/bin/launch.sh"
