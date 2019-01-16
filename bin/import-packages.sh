#!/bin/bash

basedir=$(dirname $(dirname $(readlink -fm $0)))

source $basedir/bin/java-config.sh

mkdir -p $basedir/import
cd $basedir/import

if [ ! -d "$basedir/i2p.i2p" ]; then
  git clone https://github.com/i2p/i2p.i2p.git
fi

git checkout tags/i2p-0.9.37

if [ ! -d $basedir/jdks" ]; then
  mkdir -p jdks
  mkdir -p jdks/linux jdks/mac jdks/win
  wget --directory-prefix=jdks/linux $JDK_DOWNLOAD_URL_LINUX
  wget --directory-prefix=jdks/mac $JDK_DOWNLOAD_URL_MAC
  wget --directory-prefix=jdks/win $JDK_DOWNLOAD_URL_WIN

  tar zxvf jdks/linux/$JDK_DOWNLOAD_FILENAME_LINUX -C jdks/linux/
  tar zxvf jdks/mac/$JDK_DOWNLOAD_FILENAME_MAC -C jdks/mac/
  unzip jdks/win/$JDK_DOWNLOAD_FILENAME_WIN -d jdks/win/
fi

if [ ! -d "$basedir/apache-ant-1.10.5" ]; then
  wget https://www-us.apache.org/dist/ant/binaries/apache-ant-1.10.5-bin.tar.gz
  tar zxvf apache-ant-1.10.5-bin.tar.gz
fi


