#!/bin/bash

basedir=$(dirname $(dirname $(readlink -fm $0)))

jarPaths=`find $basedir/import/lib -name '*.jar'`

mkdir -p $basedir/target/modules
rm -f $basedir/target/modules/*

for jarPath in $jarPaths; do
  moduleName=$(basename "${jarPath%.*}")
  echo "*** Determining dependencies for $moduleName"
  jdeps --module-path $basedir/import/lib --add-modules=ALL-MODULE-PATH --generate-module-info $basedir/target/module-info $jarPath
done
for jarPath in $jarPaths; do
  moduleName=$(basename "${jarPath%.*}")
  echo "*** Creating new modular jar for $moduleName"
  javac --module-path $basedir/import/lib --patch-module $moduleName=$jarPath $basedir/target/module-info/$moduleName/module-info.java
  cp $jarPath $basedir/target/modules/
  jar uf $basedir/target/modules/${moduleName}.jar -C $basedir/target/module-info/$moduleName module-info.class
done
