#!/bin/bash
set -e
set -o pipefail

if [ $(uname -s) = Darwin ]; then
    basedir=$(dirname $(cd "$(dirname "$0")"; pwd -P))
else
    basedir=$(dirname $(dirname $(readlink -fm $0)))
fi

export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8

# Get latest JDK version from https://adoptopenjdk.net/releases.html?variant=openjdk15&jvmVariant=hotspot
JDK_VERSION=14.0.2+12
JDK_MAJOR_VERSION=`echo $JDK_VERSION | cut -f1 -d"." | cut -f1 -d+`
JDK_VERSION_URL_ENC=`echo "$JDK_VERSION" | sed 's/+/%2B/g'`
JDK_VERSION_URL_ENC2=`echo "$JDK_VERSION" | sed 's/+/_/g'`
JDK_DOWNLOAD_URL_LINUX=https://github.com/AdoptOpenJDK/openjdk${JDK_MAJOR_VERSION}-binaries/releases/download/jdk-${JDK_VERSION_URL_ENC}/OpenJDK${JDK_MAJOR_VERSION}U-jdk_x64_linux_hotspot_${JDK_VERSION_URL_ENC2}.tar.gz
JDK_DOWNLOAD_URL_MAC=https://github.com/AdoptOpenJDK/openjdk${JDK_MAJOR_VERSION}-binaries/releases/download/jdk-${JDK_VERSION_URL_ENC}/OpenJDK${JDK_MAJOR_VERSION}U-jdk_x64_mac_hotspot_${JDK_VERSION_URL_ENC2}.tar.gz
JDK_DOWNLOAD_URL_WIN=https://github.com/AdoptOpenJDK/openjdk${JDK_MAJOR_VERSION}-binaries/releases/download/jdk-${JDK_VERSION_URL_ENC}/OpenJDK${JDK_MAJOR_VERSION}U-jdk_x64_windows_hotspot_${JDK_VERSION_URL_ENC2}.zip

JDK_DOWNLOAD_FILENAME_LINUX="${JDK_DOWNLOAD_URL_LINUX##*/}"
JDK_DOWNLOAD_FILENAME_MAC="${JDK_DOWNLOAD_URL_MAC##*/}"
JDK_DOWNLOAD_FILENAME_WIN="${JDK_DOWNLOAD_URL_WIN##*/}"

JAVA_HOME_LINUX=$basedir/import/jdks/linux/jdk-$JDK_VERSION
JAVA_HOME_MAC=$basedir/import/jdks/mac/jdk-$JDK_VERSION/Contents/Home
JAVA_HOME_WIN=$basedir/import/jdks/win/jdk-$JDK_VERSION

# JavaFX from https://gluonhq.com/products/javafx/
JAVAFX_VERSION=15.0.1
JAVAFX_SDK_DOWNLOAD_URL_LINUX=https://download2.gluonhq.com/openjfx/$JAVAFX_VERSION/openjfx-${JAVAFX_VERSION}_linux-x64_bin-sdk.zip
JAVAFX_SDK_DOWNLOAD_URL_MAC=https://download2.gluonhq.com/openjfx/$JAVAFX_VERSION/openjfx-${JAVAFX_VERSION}_osx-x64_bin-sdk.zip
JAVAFX_SDK_DOWNLOAD_URL_WIN=https://download2.gluonhq.com/openjfx/$JAVAFX_VERSION/openjfx-${JAVAFX_VERSION}_windows-x64_bin-sdk.zip

JAVAFX_JMODS_DOWNLOAD_URL_LINUX=https://download2.gluonhq.com/openjfx/$JAVAFX_VERSION/openjfx-${JAVAFX_VERSION}_linux-x64_bin-jmods.zip
JAVAFX_JMODS_DOWNLOAD_URL_MAC=https://download2.gluonhq.com/openjfx/$JAVAFX_VERSION/openjfx-${JAVAFX_VERSION}_osx-x64_bin-jmods.zip
JAVAFX_JMODS_DOWNLOAD_URL_WIN=https://download2.gluonhq.com/openjfx/$JAVAFX_VERSION/openjfx-${JAVAFX_VERSION}_windows-x64_bin-jmods.zip

JAVAFX_JMODS_DOWNLOAD_FILENAME_LINUX=openjfx-${JAVAFX_VERSION}_linux-x64_bin-jmods.zip
JAVAFX_JMODS_DOWNLOAD_FILENAME_MAC=openjfx-${JAVAFX_VERSION}_osx-x64_bin-jmods.zip
JAVAFX_JMODS_DOWNLOAD_FILENAME_WIN=openjfx-${JAVAFX_VERSION}_windows-x64_bin-jmods.zip


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