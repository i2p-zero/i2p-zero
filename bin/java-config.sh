#!/bin/bash

if [ $(uname -s) = Darwin ]; then
    basedir=$(dirname $(cd "$(dirname "$0")"; pwd -P))
else
    basedir=$(dirname $(dirname $(readlink -fm $0)))
fi

export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8

JDK_DOWNLOAD_FILENAME_LINUX=OpenJDK13U-jdk_x64_linux_hotspot_13.0.2_8.tar.gz
JDK_DOWNLOAD_FILENAME_MAC=OpenJDK13U-jdk_x64_mac_hotspot_13.0.2_8.tar.gz
JDK_DOWNLOAD_FILENAME_WIN=OpenJDK13U-jdk_x86-32_windows_hotspot_13.0.2_8.zip

JDK_DOWNLOAD_URL_LINUX=https://github.com/AdoptOpenJDK/openjdk13-binaries/releases/download/jdk-13.0.2%2B8/OpenJDK13U-jdk_x64_linux_hotspot_13.0.2_8.tar.gz
JDK_DOWNLOAD_URL_MAC=https://github.com/AdoptOpenJDK/openjdk13-binaries/releases/download/jdk-13.0.2%2B8/OpenJDK13U-jdk_x64_mac_hotspot_13.0.2_8.tar.gz
JDK_DOWNLOAD_URL_WIN=https://github.com/AdoptOpenJDK/openjdk13-binaries/releases/download/jdk-13.0.2%2B8/OpenJDK13U-jdk_x86-32_windows_hotspot_13.0.2_8.zip

JAVA_HOME_LINUX=$basedir/import/jdks/linux/jdk-13.0.2+8
JAVA_HOME_MAC=$basedir/import/jdks/mac/jdk-13.0.2+8/Contents/Home
JAVA_HOME_WIN=$basedir/import/jdks/win/jdk-13.0.2+8

JAVAFX_VERSION=13
JAVAFX_SDK_DOWNLOAD_URL_LINUX=https://download2.gluonhq.com/openjfx/$JAVAFX_VERSION/openjfx-${JAVAFX_VERSION}_linux-x64_bin-sdk.zip
JAVAFX_SDK_DOWNLOAD_URL_MAC=https://download2.gluonhq.com/openjfx/$JAVAFX_VERSION/openjfx-${JAVAFX_VERSION}_osx-x64_bin-sdk.zip
JAVAFX_SDK_DOWNLOAD_URL_WIN=https://download2.gluonhq.com/openjfx/$JAVAFX_VERSION/openjfx-${JAVAFX_VERSION}_windows-x64_bin-sdk.zip

JAVAFX_JMODS_DOWNLOAD_URL_LINUX=https://download2.gluonhq.com/openjfx/$JAVAFX_VERSION/openjfx-${JAVAFX_VERSION}_linux-x64_bin-jmods.zip
JAVAFX_JMODS_DOWNLOAD_URL_MAC=https://download2.gluonhq.com/openjfx/$JAVAFX_VERSION/openjfx-${JAVAFX_VERSION}_osx-x64_bin-jmods.zip
JAVAFX_JMODS_DOWNLOAD_URL_WIN=https://download2.gluonhq.com/openjfx/$JAVAFX_VERSION/openjfx-${JAVAFX_VERSION}_windows-x64_bin-jmods.zip

JAVAFX_JMODS_DOWNLOAD_FILENAME_LINUX=openjfx-${JAVAFX_VERSION}_linux-x64_bin-jmods.zip
JAVAFX_JMODS_DOWNLOAD_FILENAME_MAC=openjfx-${JAVAFX_VERSION}_osx-x64_bin-jmods.zip
JAVAFX_JMODS_DOWNLOAD_FILENAME_WIN=openjfx-${JAVAFX_VERSION}_windows-x64_bin-jmods.zip

JPACKAGER_DOWNLOAD_URL_LINUX=http://download2.gluonhq.com/jpackager/11/jdk.packager-linux.zip
JPACKAGER_DOWNLOAD_URL_WIN=http://download2.gluonhq.com/jpackager/11/jdk.packager-windows.zip

JPACKAGER_DOWNLOAD_FILENAME_LINUX=jdk.packager-linux.zip
JPACKAGER_DOWNLOAD_FILENAME_WIN=jdk.packager-windows.zip


OS=`uname -s`
if [ $OS = "Darwin" ]; then
  export JAVA_HOME=$JAVA_HOME_MAC
else
  export JAVA_HOME=$JAVA_HOME_LINUX
fi

# get the SHA-256 hash of the specified file
getHash () {
  if [ $(uname -s) = Darwin ]; then
    h=`shasum -a 256 $1 | awk '{print $1}'`
  else
    h=`sha256sum $1 | awk '{print $1}'`
  fi
  echo $h
}

# normalizes the specified jar or zip for reproducible build. Enforces consistent zip file order and sets all timestamps to midnight on Jan 1 2019
normalizeZip () {
  $JAVA_HOME/bin/java --module-path "$basedir/import/commons-compress-1.20/commons-compress-1.20.jar":"$basedir/target/org.getmonero.util.normalizeZip.jar" \
  -m org.getmonero.util.normalizeZip 1546300800000 "$1"
}