#!/bin/bash

if [ $(uname -s) = Darwin ]; then
    basedir=$(dirname $(cd "$(dirname "$0")"; pwd -P))
else
    basedir=$(dirname $(dirname $(readlink -fm $0)))
fi

rm -fr "$basedir/dist-zip" "$basedir/dist-zip-staging" "$basedir/dist" "$basedir/target" "$basedir/import" "$basedir/out"