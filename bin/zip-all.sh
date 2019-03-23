#!/bin/bash

if [ $(uname -s) = Darwin ]; then
    basedir=$(dirname $(cd "$(dirname "$0")"; pwd -P))
else
    basedir=$(dirname $(dirname $(readlink -fm $0)))
fi

rm -fr "$basedir/dist-zip-staging"
mkdir -p "$basedir/dist-zip-staging"

rm -fr "$basedir/dist-zip"
mkdir -p "$basedir/dist-zip"

VERSION=$(head -n 1 "$basedir/org.getmonero.i2p.zero/src/org/getmonero/i2p/zero/VERSION")

cd "$basedir/dist"

for i in linux linux-gui mac mac-gui win win-gui; do cp -r ${i} "$basedir"/dist-zip-staging/i2p-zero-${i}.v${VERSION}; done

cd "$basedir/dist-zip-staging"

for i in win win-gui; do zip -r9 "$basedir"/dist-zip/i2p-zero-${i}.v${VERSION}.zip i2p-zero-${i}.v${VERSION}; done

if [ $(uname -s) = Darwin ]; then
    for i in linux linux-gui mac mac-gui; do tar -jcvf "$basedir"/dist-zip/i2p-zero-${i}.v${VERSION}.tar.bz2 i2p-zero-${i}.v${VERSION}; done
else
    for i in linux linux-gui mac mac-gui; do tar --{owner,group}=nobody -jcvf "$basedir"/dist-zip/i2p-zero-${i}.v${VERSION}.tar.bz2 i2p-zero-${i}.v${VERSION}; done
fi

cd ..

du -sh  "$basedir/dist-zip/"*.zip
du -sh  "$basedir/dist-zip/"*.tar.bz2