#!/bin/bash
set -e
set -o pipefail

if [ $(uname -s) = Darwin ]; then
    basedir=$(dirname $(cd "$(dirname "$0")"; pwd -P))
else
    basedir=$(dirname $(dirname $(readlink -fm $0)))
fi

source "$basedir/bin/java-config.sh"

cd "$basedir/import"

# build the jars we're going to modularize
cd "$basedir/import/i2p.i2p"
export LG2=en
"$basedir"/import/apache-ant-1.10.7/bin/ant buildRouter buildI2PTunnelJars buildSAM jbigi buildAddressbook
cd ..


# copy the jars that we're going to modularize
rm -fr "$basedir/import/lib"
mkdir -p "$basedir/import/lib"
for i in addressbook.jar i2ptunnel.jar i2p.jar mstreaming.jar router.jar sam.jar streaming.jar; do cp "$basedir/import/i2p.i2p/build/$i" "$basedir/import/lib/"; done

# build a minimal i2p.base dir
rm -fr "$basedir/import/i2p.base"
mkdir -p "$basedir/import/i2p.base"
cp "$basedir/import/i2p.i2p/LICENSE.txt" "$basedir/import/i2p.base/"
cp "$basedir/import/i2p.i2p/build/jbigi.jar" "$basedir/import/i2p.base/"
for i in blocklist.txt hosts.txt certificates; do cp -r "$basedir/import/i2p.i2p/installer/resources/$i" "$basedir/import/i2p.base/"; done

mkdir -p "$basedir/import/i2p.base/geoip"
for i in dohservers.txt countries.txt; do cp -r "$basedir/import/i2p.i2p/core/resources/$i" "$basedir/import/i2p.base/geoip/"; done
cp "$basedir/import/i2p.i2p/router/resources/continents.txt" "$basedir/import/i2p.base/geoip/"
