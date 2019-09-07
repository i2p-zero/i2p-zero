#!/bin/bash

exec 3<>/dev/tcp/localhost/8051; echo "$1 $2 $3 $4 $5 $6" >&3; cat <&3