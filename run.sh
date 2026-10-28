#!/bin/sh
# Usage: ./run.sh           interactive menu
#        ./run.sh --demo    non-interactive tour of every feature
[ -d out ] || ./build.sh
java -cp out com.studyflow.Main "$@"
