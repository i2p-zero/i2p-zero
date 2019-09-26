#!/bin/bash

if [ $(uname -s) = Darwin ]; then
    basedir=$(dirname $(cd "$(dirname "$0")"; pwd -P))
else
    basedir=$(dirname $(dirname $(readlink -fm $0)))
fi

source "$basedir/bin/java-config.sh"

echo "*** Compiling Zip normalizer utility"
"$JAVA_HOME"/bin/javac --module-path import/commons-compress-1.19/commons-compress-1.19.jar -d target/classes/org.getmonero.util.normalizeZip $(find org.getmonero.util.normalizeZip/src -name '*.java')

echo "*** Packaging Zip normalizer as a modular jar"
"$JAVA_HOME"/bin/jar --create --file target/org.getmonero.util.normalizeZip.jar --main-class org.getmonero.util.normalizeZip.NormalizeZip -C target/classes/org.getmonero.util.normalizeZip .


