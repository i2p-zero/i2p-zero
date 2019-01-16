#!/bin/bash

basedir=$(dirname $(dirname $(readlink -fm $0)))

JDK_VERSION=11.0.2

JDK_DOWNLOAD_FILENAME_LINUX=openjdk-${JDK_VERSION}_linux-x64_bin.tar.gz
JDK_DOWNLOAD_FILENAME_MAC=openjdk-${JDK_VERSION}_osx-x64_bin.tar.gz
JDK_DOWNLOAD_FILENAME_WIN=openjdk-${JDK_VERSION}_windows-x64_bin.zip

JDK_DOWNLOAD_URL_LINUX=https://download.java.net/java/GA/jdk11/7/GPL/$JDK_DOWNLOAD_FILENAME_LINUX
JDK_DOWNLOAD_URL_MAC=https://download.java.net/java/GA/jdk11/7/GPL/$JDK_DOWNLOAD_FILENAME_MAC
JDK_DOWNLOAD_URL_WIN=https://download.java.net/java/GA/jdk11/7/GPL/$JDK_DOWNLOAD_FILENAME_WIN

OS=`uname -s`
if [ $OS = "Darwin" ]; then
  export JAVA_HOME=`realpath $basedir/import/jdks/mac/jdk-${JDK_VERSION}.jdk/Contents/Home`
else
  export JAVA_HOME=`realpath $basedir/import/jdks/linux/jdk-${JDK_VERSION}`
fi
