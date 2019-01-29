#!/bin/bash

wd=${0%/*}
cd "$wd"
basedir="$wd/../../../.."
./java -cp $basedir/i2p.base/jbigi.jar -m org.getmonero.i2p.zero.gui --i2p.dir.base=$basedir/i2p.base --i2p.dir.config=$basedir/i2p.config
