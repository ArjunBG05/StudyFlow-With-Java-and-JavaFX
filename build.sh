#!/bin/sh
# Compile all sources into ./out   (needs JDK 11 or newer)
set -e
rm -rf out
javac -d out $(find src test -name "*.java")
echo "Build OK -> ./out"
