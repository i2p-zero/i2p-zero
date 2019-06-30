#!/bin/bash

# get the SHA-256 hash of the specified file
getHash () {
  if [ $(uname -s) = Darwin ]; then
    h=`shasum -a 256 $1 | awk '{print $1}'`
  else
    h=`sha256sum $1 | awk '{print $1}'`
  fi
  echo $h
}

# normalizes the specified jar or zip for reproducible build. Enforces consistent zip file order and sets all timestamps to the last modified date of the VERSION file
normalizeZip () {
  java --module-path "$basedir/import/commons-compress-1.18/commons-compress-1.18.jar":"$basedir/target/org.getmonero.util.normalizeZip.jar" \
  -m org.getmonero.util.normalizeZip "$basedir/org.getmonero.i2p.zero/src/org/getmonero/i2p/zero/VERSION" "$1"
}