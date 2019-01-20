#!/bin/bash

basedir=$(dirname $(dirname $(readlink -fm $0)))

$basedir/bin/java -cp $basedir/i2p.base/jbigi.jar -m org.getmonero.i2p.zero --i2p.dir.base=$basedir/i2p.base --i2p.dir.config=$basedir/i2p.config