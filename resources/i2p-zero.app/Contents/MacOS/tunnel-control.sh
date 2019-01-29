#!/bin/bash

exec 3<>/dev/tcp/localhost/30000; echo "$1 $2 $3" >&3; cat <&3