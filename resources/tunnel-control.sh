#!/bin/bash

exec 3<>/dev/tcp/localhost/31337; echo "$1 $2 $3 $4" >&3; cat <&3