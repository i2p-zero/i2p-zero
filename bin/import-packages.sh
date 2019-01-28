#!/bin/bash

if [ $(uname -s) = Darwin ]; then
    basedir=$(dirname $(cd "$(dirname "$0")"; pwd -P))
else
    basedir=$(dirname $(dirname $(readlink -fm $0)))
fi

source $basedir/bin/java-config.sh

mkdir -p $basedir/import
cd $basedir/import

if [ ! -d "$basedir/import/i2p.i2p" ]; then
  git clone https://github.com/i2p/i2p.i2p.git
fi

cd $basedir/import/i2p.i2p
git fetch
git checkout tags/i2p-0.9.38
cd ..

if [ ! -d "$basedir/import/jdks" ]; then
  mkdir -p jdks
  mkdir -p jdks/linux jdks/mac jdks/win
  wget --directory-prefix=jdks/linux $JDK_DOWNLOAD_URL_LINUX
  wget --directory-prefix=jdks/mac $JDK_DOWNLOAD_URL_MAC
  wget --directory-prefix=jdks/win $JDK_DOWNLOAD_URL_WIN

  tar zxvf jdks/linux/$JDK_DOWNLOAD_FILENAME_LINUX -C jdks/linux/
  tar zxvf jdks/mac/$JDK_DOWNLOAD_FILENAME_MAC -C jdks/mac/
  unzip jdks/win/$JDK_DOWNLOAD_FILENAME_WIN -d jdks/win/
fi

if [ ! -d "$basedir/import/apache-ant-1.10.5" ]; then
  wget https://www-us.apache.org/dist/ant/binaries/apache-ant-1.10.5-bin.tar.gz
  tar zxvf apache-ant-1.10.5-bin.tar.gz
fi

if [ ! -d "$basedir/import/javafx-sdk-11.0.2" ]; then
  if [ $(uname -s) = Darwin ]; then
    wget $JAVAFX_SDK_DOWNLOAD_URL_MAC
    unzip openjfx-11.0.2_osx-x64_bin-sdk.zip
  else
    wget $JAVAFX_SDK_DOWNLOAD_URL_LINUX
    unzip openjfx-11.0.2_linux-x64_bin-sdk.zip
  fi
fi

if [ ! -d "$basedir/import/javafx-jmods" ]; then
  mkdir -p javafx-jmods
  mkdir -p javafx-jmods/linux javafx-jmods/mac javafx-jmods/win
  wget --directory-prefix=javafx-jmods/linux $JAVAFX_JMODS_DOWNLOAD_URL_LINUX
  wget --directory-prefix=javafx-jmods/mac $JAVAFX_JMODS_DOWNLOAD_URL_MAC
  wget --directory-prefix=javafx-jmods/win $JAVAFX_JMODS_DOWNLOAD_URL_WIN

  unzip jdks/linux/$JAVAFX_JMODS_DOWNLOAD_FILENAME_LINUX -d javafx-jmods/linux/
  unzip jdks/mac/$JAVAFX_JMODS_DOWNLOAD_FILENAME_MAC -d javafx-jmods/mac/
  unzip jdks/win/$JAVAFX_JMODS_DOWNLOAD_FILENAME_WIN -d javafx-jmods/win/
fi




