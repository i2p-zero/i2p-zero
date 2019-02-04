#!/bin/bash

if [ $(uname -s) = Darwin ]; then
    basedir=$(dirname $(cd "$(dirname "$0")"; pwd -P))
else
    basedir=$(dirname $(dirname $(readlink -fm $0)))
fi

source "$basedir/bin/java-config.sh"


echo "*** Compiling CLI"
"$JAVA_HOME"/bin/javac --module-path target/modules/combined.jar -d target/classes/org.getmonero.i2p.zero $(find org.getmonero.i2p.zero/src -name '*.java')

echo "*** Packaging CLI as a modular jar"
"$JAVA_HOME"/bin/jar --create --file target/org.getmonero.i2p.zero.jar --main-class org.getmonero.i2p.zero.Main -C target/classes/org.getmonero.i2p.zero .

echo "*** Compiling GUI"
"$JAVA_HOME"/bin/javac --module-path target/org.getmonero.i2p.zero.jar:target/modules/combined.jar:import/javafx-sdks/linux/javafx-sdk-$JAVAFX_VERSION/lib -d target/classes/org.getmonero.i2p.zero.gui $(find org.getmonero.i2p.zero.gui/src -name '*.java')

cp -r org.getmonero.i2p.zero.gui/src/org/getmonero/i2p/zero/gui/*.{css,png,fxml,ttf} target/classes/org.getmonero.i2p.zero.gui/org/getmonero/i2p/zero/gui/

echo "*** Packaging GUI as a modular jar"
"$JAVA_HOME"/bin/jar --create --file target/org.getmonero.i2p.zero.gui.jar --main-class org.getmonero.i2p.zero.gui.Gui -C target/classes/org.getmonero.i2p.zero.gui .


rm -fr "$basedir/dist"
for i in linux mac win linux-gui mac-gui win-gui; do mkdir -p "$basedir/dist/$i"; done

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
  "$JAVA_HOME"/bin/jlink --module-path "${JAVA_HOME_VARIANT}/jmods":target/modules:target/org.getmonero.i2p.zero.jar --add-modules combined,org.getmonero.i2p.zero --output dist/$i/router --compress 2 --no-header-files --no-man-pages
  "$JAVA_HOME"/bin/jlink --module-path "${JAVA_HOME_VARIANT}/jmods":import/javafx-jmods/$i/javafx-jmods-${JAVAFX_VERSION}:target/modules:target/org.getmonero.i2p.zero.jar:target/org.getmonero.i2p.zero.gui.jar --add-modules combined,org.getmonero.i2p.zero,org.getmonero.i2p.zero.gui,javafx.controls,javafx.fxml,java.desktop --output dist/$i-gui/router --compress 2 --no-header-files --no-man-pages
done

for i in linux mac linux-gui mac-gui; do
  cp "$basedir/resources/launch.sh" "$basedir/dist/$i/router/bin/"
  cp "$basedir/resources/tunnel-control.sh" "$basedir/dist/$i/router/bin/"
done
for i in win win-gui; do
  cp "$basedir/resources/launch.bat" "$basedir/dist/$i/router/bin/"
done

for i in linux-gui mac-gui; do
  cp "$basedir/resources/launch-gui.sh" "$basedir/dist/$i/router/bin/"
done
for i in win-gui; do
  cp "$basedir/resources/launch-gui.bat" "$basedir/dist/$i/router/bin/"
done


for i in linux mac win linux-gui mac-gui win-gui; do cp -r "$basedir/import/i2p.base" "$basedir/dist/$i/router/"; done

# remove unnecessary native libs from jbigi.jar
for i in linux mac win; do
  for j in freebsd linux mac win; do
    if [ "$i" != "$j" ]; then
      if [ "$j" = "mac" ]; then j="osx"; fi
      if [ "$j" = "win" ]; then j="windows"; fi
      zip -d "$basedir/dist/$i/router/i2p.base/jbigi.jar" *-${j}-*
      zip -d "$basedir/dist/$i-gui/router/i2p.base/jbigi.jar" *-${j}-*
    fi
  done
done

# build map app structure
mv "$basedir/dist/mac-gui/router" "$basedir/dist/mac-gui/router-tmp"
mkdir -p "$basedir/dist/mac-gui/router/i2p-zero.app/Contents/MacOS/"
cp -R "$basedir/resources/i2p-zero.app" "$basedir/dist/mac-gui/router/"
mv "$basedir/dist/mac-gui/router-tmp"/* "$basedir/dist/mac-gui/router/i2p-zero.app/Contents/MacOS/"
rm -fr "$basedir/dist/mac-gui/router-tmp"

# specify .desktop file so linux-gui can be launched via double-click
cp "$basedir/resources/i2p-zero.desktop" "$basedir/dist/linux-gui/router/"

du -sk dist/* | awk '{printf "%.1f MB %s\n",$1/1024,$2}'

echo "*** Done ***"
os_name=`uname -s`
if [ $os_name = Darwin ]; then
    os_name=mac
    echo "To run from the command line, type: ./dist/$os_name/router/bin/launch.sh"
    echo "To run the GUI, double-click: dist/$os_name-gui/router/i2p-zero.app"
elif [ $os_name = Linux ]; then
    os_name=linux
    echo "To run from the command line, type: ./dist/$os_name/router/bin/launch.sh"
    echo "To run the GUI, type: dist/$os_name-gui/router/bin/launch-gui.sh"
else
    os_name=win
    echo "To run from the command line, type: ./dist/$os_name/router/bin/launch.bat"
    echo "To run the GUI, double-click: dist/$os_name-gui/router/bin/launch-gui.bat"
fi
