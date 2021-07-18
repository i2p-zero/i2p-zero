#!/bin/bash
set -e
set -o pipefail

if [ $(uname -s) = Darwin ]; then
    basedir=$(dirname $(cd "$(dirname "$0")"; pwd -P))
else
    basedir=$(dirname $(dirname $(readlink -fm $0)))
fi

export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8

: "${TARGETS:="linux mac win"}"
declare -A variables=()

# Get latest JDK version from https://adoptopenjdk.net/releases.html?variant=openjdk15&jvmVariant=hotspot
JDK_VERSION=14.0.2+12
JDK_MAJOR_VERSION=`echo $JDK_VERSION | cut -f1 -d"." | cut -f1 -d+`
JDK_VERSION_URL_ENC=`echo "$JDK_VERSION" | sed 's/+/%2B/g'`
JDK_VERSION_URL_ENC2=`echo "$JDK_VERSION" | sed 's/+/_/g'`

variables["JDK_DOWNLOAD_FILENAME_linux"]="OpenJDK${JDK_MAJOR_VERSION}U-jdk_x64_linux_hotspot_${JDK_VERSION_URL_ENC2}.tar.gz"
variables["JDK_DOWNLOAD_FILENAME_mac"]="OpenJDK${JDK_MAJOR_VERSION}U-jdk_x64_mac_hotspot_${JDK_VERSION_URL_ENC2}.tar.gz"
variables["JDK_DOWNLOAD_FILENAME_win"]="OpenJDK${JDK_MAJOR_VERSION}U-jdk_x64_windows_hotspot_${JDK_VERSION_URL_ENC2}.zip"

variables["JAVA_HOME_linux"]="jdk-$JDK_VERSION"
variables["JAVA_HOME_mac"]="jdk-$JDK_VERSION/Contents/Home"
variables["JAVA_HOME_win"]="jdk-$JDK_VERSION"

JAVAFX_VERSION="15.0.1"

variables["JAVAFX_SDK_FILENAME_linux"]="openjfx-${JAVAFX_VERSION}_linux-x64_bin-sdk.zip"
variables["JAVAFX_SDK_FILENAME_mac"]="openjfx-${JAVAFX_VERSION}_osx-x64_bin-sdk.zip"
variables["JAVAFX_SDK_FILENAME_win"]="openjfx-${JAVAFX_VERSION}_windows-x64_bin-sdk.zip"

variables["JAVAFX_JMODS_FILENAME_linux"]="openjfx-${JAVAFX_VERSION}_linux-x64_bin-jmods.zip"
variables["JAVAFX_JMODS_FILENAME_mac"]="openjfx-${JAVAFX_VERSION}_osx-x64_bin-jmods.zip"
variables["JAVAFX_JMODS_FILENAME_win"]="openjfx-${JAVAFX_VERSION}_windows-x64_bin-jmods.zip"

variables["DECOMPRESS_linux"]="tar -C jdks/linux -zxf"
variables["DECOMPRESS_mac"]="tar -C jdks/mac -zxf"
variables["DECOMPRESS_win"]="unzip -q -d jdks/win"

OS=`uname -s`
if [ $OS = "Darwin" ]; then
  export JAVA_HOME=$basedir/import/jdks/mac/${variables["JAVA_HOME_mac"]}
else
  export JAVA_HOME=$basedir/import/jdks/linux/${variables["JAVA_HOME_linux"]}
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