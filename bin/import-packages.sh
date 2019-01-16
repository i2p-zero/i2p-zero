#!/bin/bash

basedir=$(dirname $(dirname $(readlink -fm $0)))

mkdir $basedir/import
cd $basedir/import

git clone https://github.com/i2p/i2p.i2p.git

wget https://download.java.net/java/GA/jdk11/7/GPL/openjdk-11.0.2_linux-x64_bin.tar.gz
wget https://www-us.apache.org/dist//ant/binaries/apache-ant-1.10.5-bin.tar.gz

tar zxvf openjdk-11.0.2_linux-x64_bin.tar.gz
tar zxvf apache-ant-1.10.5-bin.tar.gz