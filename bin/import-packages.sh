#!/bin/bash

basedir=$(dirname $(dirname $(readlink -fm $0)))

mkdir $basedir/import
cd $basedir/import

git clone https://github.com/i2p/i2p.i2p.git

mkdir jdks
mkdir jdks/linux jdks/mac jdks/win
wget --directory-prefix=jdks/linux https://download.java.net/java/GA/jdk11/7/GPL/openjdk-11.0.2_linux-x64_bin.tar.gz
wget --directory-prefix=jdks/mac https://download.java.net/java/GA/jdk11/7/GPL/openjdk-11.0.2_osx-x64_bin.tar.gz
wget --directory-prefix=jdks/win https://download.java.net/java/GA/jdk11/7/GPL/openjdk-11.0.2_windows-x64_bin.zip

wget https://www-us.apache.org/dist/ant/binaries/apache-ant-1.10.5-bin.tar.gz

tar zxvf jdks/linux/openjdk-11.0.2_linux-x64_bin.tar.gz -C jdks/linux/
tar zxvf jdks/mac/openjdk-11.0.2_osx-x64_bin.tar.gz -C jdks/mac/
unzip jdks/win/openjdk-11.0.2_windows-x64_bin.zip -d jdks/win/

tar zxvf apache-ant-1.10.5-bin.tar.gz