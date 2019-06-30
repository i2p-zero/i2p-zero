#!/bin/bash

if [ $(uname -s) = Darwin ]; then
    basedir=$(dirname $(cd "$(dirname "$0")"; pwd -P))
else
    basedir=$(dirname $(dirname $(readlink -fm $0)))
fi

source "$basedir/bin/java-config.sh"
source "$basedir/bin/util.sh"

echo "*** Compiling CLI"
"$JAVA_HOME"/bin/javac --module-path target/modules/combined.jar -d target/classes/org.getmonero.i2p.zero $(find org.getmonero.i2p.zero/src -name '*.java')
cp org.getmonero.i2p.zero/src/org/getmonero/i2p/zero/VERSION target/classes/org.getmonero.i2p.zero/org/getmonero/i2p/zero/

echo "*** Packaging CLI as a modular jar"
"$JAVA_HOME"/bin/jar --create --file target/org.getmonero.i2p.zero.jar --main-class org.getmonero.i2p.zero.Main -C target/classes/org.getmonero.i2p.zero .
normalizeZip target/org.getmonero.i2p.zero.jar

echo "*** Compiling GUI"
"$JAVA_HOME"/bin/javac --module-path target/org.getmonero.i2p.zero.jar:target/modules/combined.jar:import/javafx-sdks/linux/javafx-sdk-$JAVAFX_VERSION/lib -d target/classes/org.getmonero.i2p.zero.gui $(find org.getmonero.i2p.zero.gui/src -name '*.java')

cp -r org.getmonero.i2p.zero.gui/src/org/getmonero/i2p/zero/gui/*.{css,png,fxml,ttf} target/classes/org.getmonero.i2p.zero.gui/org/getmonero/i2p/zero/gui/

echo "*** Packaging GUI as a modular jar"
"$JAVA_HOME"/bin/jar --create --file target/org.getmonero.i2p.zero.gui.jar --main-class org.getmonero.i2p.zero.gui.Gui -C target/classes/org.getmonero.i2p.zero.gui .
normalizeZip target/org.getmonero.i2p.zero.gui.jar

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
  "$JAVA_HOME"/bin/jlink --module-path "${JAVA_HOME_VARIANT}/jmods":target/modules:target/org.getmonero.i2p.zero.jar --add-modules combined,org.getmonero.i2p.zero --output dist/$i/router --compress 2 --no-header-files --no-man-pages --order-resources=**/module-info.class,/java.base/java/lang/**,**javafx**
  "$JAVA_HOME"/bin/jlink --module-path "${JAVA_HOME_VARIANT}/jmods":import/javafx-jmods/$i/javafx-jmods-${JAVAFX_VERSION}:target/modules:target/org.getmonero.i2p.zero.jar:target/org.getmonero.i2p.zero.gui.jar --add-modules combined,org.getmonero.i2p.zero,org.getmonero.i2p.zero.gui,javafx.controls,javafx.fxml,java.desktop --output dist/$i-gui/router --compress 2 --no-header-files --no-man-pages --order-resources=**/module-info.class,/java.base/java/lang/**,**javafx**
done

for i in linux mac linux-gui mac-gui; do
  cp "$basedir/resources/launch.sh" "$basedir/dist/$i/router/bin/"
  cp "$basedir/resources/tunnel-control.sh" "$basedir/dist/$i/router/bin/"
done
for i in win; do
  cp "$basedir/resources/launch.bat" "$basedir/dist/$i/router/bin/"
done

for i in linux-gui mac-gui; do
  cp "$basedir/resources/launch-gui.sh" "$basedir/dist/$i/router/bin/"
done

for i in linux mac win linux-gui mac-gui win-gui; do cp -r "$basedir/import/i2p.base" "$basedir/dist/$i/router/"; done

# remove unnecessary native libs from jbigi.jar
for i in linux mac win; do
  for j in freebsd linux mac win; do
    if [ "$i" != "$j" ]; then
      if [ "$j" = "mac" ]; then j="osx"; fi
      if [ "$j" = "win" ]; then j="windows"; fi
      zip -d "$basedir/dist/$i/router/i2p.base/jbigi.jar" *-${j}-*
      normalizeZip "$basedir/dist/$i/router/i2p.base/jbigi.jar"
      cp "$basedir/dist/$i/router/i2p.base/jbigi.jar" "$basedir/dist/$i-gui/router/i2p.base/jbigi.jar"
    fi
  done
done



# build mac gui app structure
mv "$basedir/dist/mac-gui/router" "$basedir/dist/mac-gui/router-tmp"
mkdir -p "$basedir/dist/mac-gui/router/i2p-zero.app/Contents/MacOS/"
cp -R "$basedir/resources/i2p-zero.app" "$basedir/dist/mac-gui/router/"
mv "$basedir/dist/mac-gui/router-tmp"/* "$basedir/dist/mac-gui/router/i2p-zero.app/Contents/MacOS/"
rm -fr "$basedir/dist/mac-gui/router-tmp"


# build linux gui app structure
mv "$basedir/dist/linux-gui/router" "$basedir/dist/linux-gui/router-tmp"
mkdir -p "$basedir/dist/linux-gui/router/app"
mkdir -p "$basedir/dist/linux-gui/router/resources"

mv "$basedir/dist/linux-gui/router-tmp" "$basedir/dist/linux-gui/router/runtime"

cp "$basedir/import/javapackager/linux/jdk/packager/internal/resources/linux/papplauncher" "$basedir/dist/linux-gui/router/i2p-zero"
cp "$basedir/import/javapackager/linux/jdk/packager/internal/resources/linux/libpackager.so" "$basedir/dist/linux-gui/router/"
chmod +x "$basedir/dist/linux-gui/router/i2p-zero"

cp "$basedir/resources/i2p-zero.linux.cfg" "$basedir/dist/linux-gui/router/app/i2p-zero.cfg"
cp "$basedir/org.getmonero.i2p.zero.gui/src/org/getmonero/i2p/zero/gui/icon.png" "$basedir/dist/linux-gui/router/resources/i2p-zero.png"


# build win gui app structure
mv "$basedir/dist/win-gui/router" "$basedir/dist/win-gui/router-tmp"
mkdir -p "$basedir/dist/win-gui/router/app"

mv "$basedir/dist/win-gui/router-tmp" "$basedir/dist/win-gui/router/runtime"

cp "$basedir/resources/wrapper.exe" "$basedir/dist/win-gui/router/i2p-zero.exe"

cp "$basedir/import/javapackager/win/jdk/packager/internal/resources/windows/packager.dll" "$basedir/dist/win-gui/router/"
for i in msvcp140.dll vcruntime140.dll; do
  cp "$basedir/dist/win-gui/router/runtime/bin/$i"  "$basedir/dist/win-gui/router/"
done

cp "$basedir/resources/i2p-zero.win.cfg" "$basedir/dist/win-gui/router/app/i2p-zero.cfg"


# linux-gui launcher: fix location of libjli.so due to slight incompatibility with javapackager11 when used with jdk12 hotspot VM
mkdir "$basedir/dist/linux-gui/router/runtime/lib/jli"
cp "$basedir/dist/linux-gui/router/runtime/lib/libjli.so" "$basedir/dist/linux-gui/router/runtime/lib/jli/"


# show distribution sizes
du -sk dist/* | awk '{printf "%.1f MB %s\n",$1/1024,$2}'


echo "*** Done ***"
echo "To build the distribution archives and show reproducible build SHA-256 hashes, type: bin/zip-all.sh"
echo ""
os_name=`uname -s`
if [ $os_name = Darwin ]; then
    os_name=mac
    echo "To run from the command line, type: ./dist/$os_name/router/bin/launch.sh"
    echo "To run the GUI, double-click: dist/$os_name-gui/router/i2p-zero.app"
elif [ $os_name = Linux ]; then
    os_name=linux
    echo "To run from the command line, type: ./dist/$os_name/router/bin/launch.sh"
    echo "To run the GUI, double-click: dist/$os_name-gui/router/i2p-zero"
else
    os_name=win
    echo "To run from the command line, type: ./dist/$os_name/router/bin/launch.bat"
    echo "To run the GUI, double-click: dist/$os_name-gui/router/i2p-zero.exe"
fi
