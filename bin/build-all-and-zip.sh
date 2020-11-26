#!/bin/bash
set -e
set -o pipefail

if [ $(uname -s) = Darwin ]; then
    basedir=$(dirname $(cd "$(dirname "$0")"; pwd -P))
else
    basedir=$(dirname $(dirname $(readlink -fm $0)))
fi

"$basedir"/bin/build-all.sh
"$basedir"/bin/zip-all.sh