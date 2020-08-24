#!/bin/bash

if [ $(uname -s) = Darwin ]; then
    basedir=$(dirname $(cd "$(dirname "$0")"; pwd -P))
else
    basedir=$(dirname $(dirname $(readlink -fm $0)))
fi

source "$basedir/bin/java-config.sh"

echo "*** Compiling CLI"
"$JAVA_HOME"/bin/javac --enable-preview -source 14 --module-path "$basedir/target/modules/combined.jar" -d "$basedir/target/classes/org.getmonero.i2p.zero" $(find "$basedir/org.getmonero.i2p.zero/src" -name '*.java')
cp "$basedir/org.getmonero.i2p.zero/src/org/getmonero/i2p/zero/VERSION" "$basedir/target/classes/org.getmonero.i2p.zero/org/getmonero/i2p/zero/"

echo "*** Packaging CLI as a modular jar"
"$JAVA_HOME"/bin/jar --create --file "$basedir/target/org.getmonero.i2p.zero.jar" --main-class org.getmonero.i2p.zero.Main -C "$basedir/target/classes/org.getmonero.i2p.zero" .
normalizeZip "$basedir/target/org.getmonero.i2p.zero.jar"

echo "*** Compiling GUI"
"$JAVA_HOME"/bin/javac --enable-preview -source 14 --module-path "$basedir/target/org.getmonero.i2p.zero.jar:$basedir/target/modules/combined.jar:$basedir/import/javafx-sdks/linux/javafx-sdk-$JAVAFX_VERSION/lib" -d "$basedir/target/classes/org.getmonero.i2p.zero.gui" $(find "$basedir/org.getmonero.i2p.zero.gui/src" -name '*.java')

cp -r "$basedir/org.getmonero.i2p.zero.gui/src/org/getmonero/i2p/zero/gui/"*.{css,png,fxml,ttf} "$basedir/target/classes/org.getmonero.i2p.zero.gui/org/getmonero/i2p/zero/gui/"

echo "*** Packaging GUI as a modular jar"
"$JAVA_HOME"/bin/jar --create --file "$basedir/target/org.getmonero.i2p.zero.gui.jar" --main-class org.getmonero.i2p.zero.gui.Gui -C "$basedir/target/classes/org.getmonero.i2p.zero.gui" .
normalizeZip "$basedir/target/org.getmonero.i2p.zero.gui.jar"

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
  "$JAVA_HOME"/bin/jlink --module-path "${JAVA_HOME_VARIANT}/jmods":"$basedir/target/modules":"$basedir/target/org.getmonero.i2p.zero.jar" --add-modules combined,org.getmonero.i2p.zero --output "$basedir/dist/$i/router" --compress 2 --no-header-files --no-man-pages --order-resources=**/module-info.class,/java.base/java/lang/**,**javafx**
  "$JAVA_HOME"/bin/jlink --module-path "${JAVA_HOME_VARIANT}/jmods":"$basedir/import/javafx-jmods/$i/javafx-jmods-${JAVAFX_VERSION}":"$basedir/target/modules":"$basedir/target/org.getmonero.i2p.zero.jar":"$basedir/target/org.getmonero.i2p.zero.gui.jar" --add-modules combined,org.getmonero.i2p.zero,org.getmonero.i2p.zero.gui,javafx.controls,javafx.fxml,java.desktop --output "$basedir/dist/$i-gui/router" --compress 2 --no-header-files --no-man-pages --order-resources=**/module-info.class,/java.base/java/lang/**,**javafx**
done

for i in mac-gui; do
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


# build linux and linux-gui app structure
for i in linux linux-gui; do
  mv "$basedir/dist/$i/router" "$basedir/dist/$i/router-tmp"
  mkdir -p "$basedir/dist/$i/router/bin"
  mkdir -p "$basedir/dist/$i/router/lib"
  cp "$basedir/import/jpackage/linux/classes/jdk/incubator/jpackage/internal/resources/jpackageapplauncher" "$basedir/dist/$i/router/bin/i2p-zero"
  chmod +x "$basedir/dist/$i/router/bin/i2p-zero"
  mkdir -p "$basedir/dist/$i/router/lib/app"
  cp "$basedir/resources/i2p-zero.$i.cfg" "$basedir/dist/$i/router/lib/app/i2p-zero.cfg"
  mv "$basedir/dist/$i/router-tmp" "$basedir/dist/$i/router/lib/runtime"
  cp "$basedir/import/jpackage/linux/classes/jdk/incubator/jpackage/internal/resources/libapplauncher.so" "$basedir/dist/$i/router/lib/"
done

cp "$basedir/i2p-zero.png" "$basedir/dist/linux-gui/router/lib/"

# build win and win-gui app structure
for i in win win-gui; do
  mv "$basedir/dist/$i/router" "$basedir/dist/$i/router-tmp"
  mkdir -p "$basedir/dist/$i/router/app"
  cp "$basedir/resources/i2p-zero.$i.cfg" "$basedir/dist/$i/router/app/i2p-zero.cfg"
  mv "$basedir/dist/$i/router-tmp" "$basedir/dist/$i/router/runtime"
  cp "$basedir/resources/launcher.exe" "$basedir/dist/$i/router/i2p-zero.exe"
  cp "$basedir/import/jpackage/win/classes/jdk/incubator/jpackage/internal/resources/applauncher.dll" "$basedir/dist/$i/router/"
done

for i in linux mac; do
  cp "$basedir/resources/tunnel-control.sh" "$basedir/dist/$i/router/bin/"
done

cp "$basedir/resources/launch.sh" "$basedir/dist/mac/router/bin/"

# show distribution sizes
du -sk "$basedir/dist/"* | awk '{printf "%.1f MB %s\n",$1/1024,$2}'


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
    echo "To run from the command line, type: ./dist/$os_name/router/bin/i2p-zero"
    echo "To run the GUI, double-click: dist/$os_name-gui/router/i2p-zero"
else
    os_name=win
    echo "To run from the command line, type: ./dist/$os_name/router/i2p-zero.exe"
    echo "To run the GUI, double-click: dist/$os_name-gui/router/i2p-zero.exe"
fi
