#!/bin/bash

if [ $(uname -s) = Darwin ]; then
    basedir=$(dirname $(cd "$(dirname "$0")"; pwd -P))
else
    basedir=$(dirname $(dirname $(readlink -fm $0)))
fi

source "$basedir/bin/java-config.sh"

cp "$basedir"/import/jetty-lib/*.jar "$basedir/import/lib/"
cp "$basedir"/import/org-json/*.jar "$basedir/import/lib/"

rm -fr "$basedir/target/lib-combined"
rm -fr "$basedir/target/lib-combined-tmp"
mkdir -p "$basedir/target/lib-combined"
mkdir -p "$basedir/target/lib-combined-tmp"

jarPaths=`find "$basedir/import/lib" -name '*.jar'`
combinedJarPath="$basedir/target/lib-combined/combined.jar"
for jarPath in $jarPaths; do unzip -quo $jarPath -d "$basedir/target/lib-combined-tmp"; done
$JAVA_HOME/bin/jar cf "$combinedJarPath" -C "$basedir/target/lib-combined-tmp" .

rm -fr "$basedir/target/module-info"
mkdir -p "$basedir/target/module-info"
rm -fr "$basedir/target/modules"
mkdir -p "$basedir/target/modules"

echo "*** Determining dependencies for $combinedJarPath"
"$JAVA_HOME"/bin/jdeps --add-modules=ALL-MODULE-PATH --generate-module-info "$basedir/target/module-info" "$combinedJarPath"

modulesToRemove="java.desktop java.management java.rmi java.sql"

if [ $(uname -s) = Darwin ]; then
  sed -i '' -e '$ d' "$basedir/target/module-info/combined/module-info.java"
  for i in $modulesToRemove; do
    sed -i '' "/$i/d" "$basedir/target/module-info/combined/module-info.java"
  done
else
  sed -i '$ d' "$basedir/target/module-info/combined/module-info.java"
  for i in $modulesToRemove; do
    sed -i "/$i/d" "$basedir/target/module-info/combined/module-info.java"
  done
fi
echo 'uses org.eclipse.jetty.http.HttpFieldPreEncoder; }' >> "$basedir/target/module-info/combined/module-info.java"


echo "*** Creating new combined modular jar"
"$JAVA_HOME"/bin/javac --enable-preview -source 14 --module-path "$combinedJarPath/combined" --patch-module combined="$combinedJarPath" "$basedir/target/module-info/combined/module-info.java"
cp $combinedJarPath "$basedir/target/modules/"
"$JAVA_HOME"/bin/jar uf "$basedir/target/modules/combined.jar" -C "$basedir/target/module-info/combined" module-info.class

normalizeZip "$basedir/target/modules/combined.jar"


