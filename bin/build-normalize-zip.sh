#!/bin/bash

if [ $(uname -s) = Darwin ]; then
    basedir=$(dirname $(cd "$(dirname "$0")"; pwd -P))
else
    basedir=$(dirname $(dirname $(readlink -fm $0)))
fi

source "$basedir/bin/java-config.sh"

echo "*** Compiling Zip normalizer utility"
"$JAVA_HOME"/bin/javac --module-path "$basedir/import/commons-compress-1.20/commons-compress-1.20.jar" -d "$basedir/target/classes/org.getmonero.util.normalizeZip" $(find "$basedir/org.getmonero.util.normalizeZip/src" -name '*.java')

echo "*** Packaging Zip normalizer as a modular jar"
"$JAVA_HOME"/bin/jar --create --file "$basedir/target/org.getmonero.util.normalizeZip.jar" --main-class org.getmonero.util.normalizeZip.NormalizeZip -C "$basedir/target/classes/org.getmonero.util.normalizeZip" .


