#!/bin/bash

if [ $(uname -s) = Darwin ]; then
    basedir=$(dirname $(cd "$(dirname "$0")"; pwd -P))
else
    basedir=$(dirname $(dirname $(readlink -fm $0)))
fi

export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8

JDK_DOWNLOAD_FILENAME_LINUX=OpenJDK11U-jdk_x64_linux_hotspot_11.0.1_13.tar.gz
JDK_DOWNLOAD_FILENAME_MAC=OpenJDK11U-jdk_x64_mac_hotspot_11.0.1_13.tar.gz
JDK_DOWNLOAD_FILENAME_WIN=OpenJDK11U-jdk_x64_windows_hotspot_11.0.1_13.zip

JDK_DOWNLOAD_URL_LINUX=https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.1%2B13/OpenJDK11U-jdk_x64_linux_hotspot_11.0.1_13.tar.gz
JDK_DOWNLOAD_URL_MAC=https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.1%2B13/OpenJDK11U-jdk_x64_mac_hotspot_11.0.1_13.tar.gz
JDK_DOWNLOAD_URL_WIN=https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.1%2B13/OpenJDK11U-jdk_x64_windows_hotspot_11.0.1_13.zip

JAVA_HOME_LINUX=$basedir/import/jdks/linux/jdk-11.0.1+13
JAVA_HOME_MAC=$basedir/import/jdks/mac/jdk-11.0.1+13/Contents/Home
JAVA_HOME_WIN=$basedir/import/jdks/win/jdk-11.0.1+13

JAVAFX_VERSION=11.0.2
JAVAFX_SDK_DOWNLOAD_URL_LINUX=https://download2.gluonhq.com/openjfx/$JAVAFX_VERSION/openjfx-${JAVAFX_VERSION}_linux-x64_bin-sdk.zip
JAVAFX_SDK_DOWNLOAD_URL_MAC=https://download2.gluonhq.com/openjfx/$JAVAFX_VERSION/openjfx-${JAVAFX_VERSION}_osx-x64_bin-sdk.zip
JAVAFX_SDK_DOWNLOAD_URL_WIN=https://download2.gluonhq.com/openjfx/$JAVAFX_VERSION/openjfx-${JAVAFX_VERSION}_windows-x64_bin-sdk.zip

JAVAFX_JMODS_DOWNLOAD_URL_LINUX=https://download2.gluonhq.com/openjfx/11.0.2/openjfx-11.0.2_linux-x64_bin-jmods.zip
JAVAFX_JMODS_DOWNLOAD_URL_MAC=https://download2.gluonhq.com/openjfx/11.0.2/openjfx-11.0.2_osx-x64_bin-jmods.zip
JAVAFX_JMODS_DOWNLOAD_URL_WIN=https://download2.gluonhq.com/openjfx/11.0.2/openjfx-11.0.2_windows-x64_bin-jmods.zip

JAVAFX_JMODS_DOWNLOAD_FILENAME_LINUX=openjfx-11.0.2_linux-x64_bin-jmods.zip
JAVAFX_JMODS_DOWNLOAD_FILENAME_MAC=openjfx-11.0.2_osx-x64_bin-jmods.zip
JAVAFX_JMODS_DOWNLOAD_FILENAME_WIN=openjfx-11.0.2_windows-x64_bin-jmods.zip

OS=`uname -s`
if [ $OS = "Darwin" ]; then
  export JAVA_HOME=$JAVA_HOME_MAC
else
  export JAVA_HOME=$JAVA_HOME_LINUX
fi
