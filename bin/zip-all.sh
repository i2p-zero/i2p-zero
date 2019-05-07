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


print3ColsJustified () {
  printf "%-.14s %s" "$1                                " "| "
  printf "%-.26s %s" "$2                                " "| "
  printf "%s\n" "$3"
}
getDirSizeMB () {
  du -sk $1 | awk '{printf "%.1f",$1/1024,$2}'
}

print3ColsJustified "OS" "Uncompressed size (MB)" "Compressed size (MB)"
print3ColsJustified "------------------------" "------------------------" "------------------------"
print3ColsJustified "Mac" "`getDirSizeMB $basedir/dist/mac`" "`getDirSizeMB $basedir/dist-zip/i2p-zero-mac.v${VERSION}.tar.bz2`"
print3ColsJustified "Windows" "`getDirSizeMB $basedir/dist/win`" "`getDirSizeMB $basedir/dist-zip/i2p-zero-win.v${VERSION}.zip`"
print3ColsJustified "Linux" "`getDirSizeMB $basedir/dist/linux`" "`getDirSizeMB $basedir/dist-zip/i2p-zero-linux.v${VERSION}.tar.bz2`"
print3ColsJustified "Mac GUI" "`getDirSizeMB $basedir/dist/mac-gui`" "`getDirSizeMB $basedir/dist-zip/i2p-zero-mac-gui.v${VERSION}.tar.bz2`"
print3ColsJustified "Windows GUI" "`getDirSizeMB $basedir/dist/win-gui`" "`getDirSizeMB $basedir/dist-zip/i2p-zero-win-gui.v${VERSION}.zip`"
print3ColsJustified "Linux GUI" "`getDirSizeMB $basedir/dist/linux-gui`" "`getDirSizeMB $basedir/dist-zip/i2p-zero-linux-gui.v${VERSION}.tar.bz2`"

