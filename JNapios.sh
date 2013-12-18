#!/bin/sh
VAR='java -jar -Xmx64m @jar_file@ '
echo "Executing command: -> " $VAR $*

$VAR $* &
