#!/bin/bash

if [ $(uname -s) = Darwin ]; then
    basedir=$(dirname $(cd "$(dirname "$0")"; pwd -P))
else
    basedir=$(dirname $(dirname $(readlink -fm $0)))
fi

if [ $# -eq 0 ]; then
  echo "Specify a version number for the release zip"
  exit
fi

rm -fr "$basedir/dist-zip"
mkdir -p "$basedir/dist-zip"

VERSION=$1

cd "$basedir/dist"
for i in linux linux-gui mac mac-gui win win-gui; do zip -r9 "$basedir"/dist-zip/${i}.${VERSION}.zip ${i}; done
cd ..

du -sh  "$basedir/dist-zip/"*.zip