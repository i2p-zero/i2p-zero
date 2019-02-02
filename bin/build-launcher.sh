#!/bin/bash

if [ $(uname -s) = Darwin ]; then
    basedir=$(dirname $(cd "$(dirname "$0")"; pwd -P))
else
    basedir=$(dirname $(dirname $(readlink -fm $0)))
fi

source $basedir/bin/java-config.sh

rm -fr target/org.getmonero.i2p.zero/classes
rm -fr target/org.getmonero.i2p.zero.gui/classes

# compile the Main class that starts the I2P router and SAM listener
echo "*** Compiling Main class"
$JAVA_HOME/bin/javac --module-path import/lib -d target/org.getmonero.i2p.zero/classes $(find org.getmonero.i2p.zero/src -name '*.java')
$JAVA_HOME/bin/javac --module-path import/lib:import/javafx-sdks/linux/javafx-sdk-$JAVAFX_VERSION/lib:target/org.getmonero.i2p.zero/classes -d target/org.getmonero.i2p.zero.gui/classes $(find org.getmonero.i2p.zero.gui/src -name '*.java')
cp -r org.getmonero.i2p.zero.gui/src/* target/org.getmonero.i2p.zero.gui/classes
find target -type f -name '*.java' -delete

# package as a modular jar
echo "*** Packaging as a modular jar"
$JAVA_HOME/bin/jar --create --file target/org.getmonero.i2p.zero.jar --main-class org.getmonero.i2p.zero.Main -C target/org.getmonero.i2p.zero/classes .
$JAVA_HOME/bin/jar --create --file target/org.getmonero.i2p.zero.gui.jar --main-class org.getmonero.i2p.zero.gui.Gui -C target/org.getmonero.i2p.zero.gui/classes .

rm -fr $basedir/dist
for i in linux mac win linux-gui mac-gui win-gui; do mkdir -p $basedir/dist/$i; done

# create OS specific launchers which will bundle together the code and a minimal JVM
for i in linux mac win; do
  echo "*** Performing jlink ($i)"

  case $i in
    linux )
        JAVA_HOME_VARIANT=${JAVA_HOME_LINUX} ;;
    mac )
        JAVA_HOME_VARIANT=${JAVA_HOME_MAC} ;;
    win )
        JAVA_HOME_VARIANT=${JAVA_HOME_WIN} ;;
  esac
  echo "Using JAVA_HOME_VARIANT: $JAVA_HOME_VARIANT"
  $JAVA_HOME/bin/jlink --module-path ${JAVA_HOME_VARIANT}/jmods:target/modules:target/org.getmonero.i2p.zero.jar --add-modules org.getmonero.i2p.zero --output dist/$i/router --strip-debug --compress 2 --no-header-files --no-man-pages
  $JAVA_HOME/bin/jlink --module-path ${JAVA_HOME_VARIANT}/jmods:import/javafx-jmods/$i/javafx-jmods-${JAVAFX_VERSION}:target/modules:target/org.getmonero.i2p.zero.jar:target/org.getmonero.i2p.zero.gui.jar --add-modules org.getmonero.i2p.zero,org.getmonero.i2p.zero.gui,javafx.controls,javafx.fxml,java.desktop --output dist/$i-gui/router --strip-debug --compress 2 --no-header-files --no-man-pages
done

for i in linux mac linux-gui mac-gui; do
  cp $basedir/resources/launch.sh $basedir/dist/$i/router/bin/
  cp $basedir/resources/tunnel-control.sh $basedir/dist/$i/router/bin/
done
for i in win win-gui; do
  cp $basedir/resources/launch.bat $basedir/dist/$i/router/bin/
done

for i in linux-gui mac-gui; do
  cp $basedir/resources/launch-gui.sh $basedir/dist/$i/router/bin/
done
for i in win-gui; do
  cp $basedir/resources/launch-gui.bat $basedir/dist/$i/router/bin/
done


for i in linux mac win linux-gui mac-gui win-gui; do cp -r $basedir/import/i2p.base $basedir/dist/$i/router/; done

# remove unnecessary native libs from jbigi.jar
for i in linux mac win; do
  for j in freebsd linux mac win; do
    if [ "$i" != "$j" ]; then
      if [ "$j" = "mac" ]; then j="osx"; fi
      if [ "$j" = "win" ]; then j="windows"; fi
      zip -d $basedir/dist/$i/router/i2p.base/jbigi.jar *-${j}-*
      zip -d $basedir/dist/$i-gui/router/i2p.base/jbigi.jar *-${j}-*
    fi
  done
done

# build map app structure
mv $basedir/dist/mac-gui/router $basedir/dist/mac-gui/router-tmp
mkdir -p $basedir/dist/mac-gui/router
cp -R $basedir/resources/i2p-zero.app $basedir/dist/mac-gui/router/
mv $basedir/dist/mac-gui/router-tmp/* $basedir/dist/mac-gui/router/i2p-zero.app/Contents/MacOS/
rm -fr $basedir/dist/mac-gui/router-tmp

du -sk dist/* | awk '{printf "%.1f MB %s\n",$1/1024,$2}'

echo "*** Done ***"
echo "To run, type: dist/linux/router/bin/launch.sh"
echo "To run the GUI, type: dist/linux-gui/router/bin/launch-gui.sh"
