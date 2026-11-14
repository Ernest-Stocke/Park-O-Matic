#!/bin/bash
# Compiles the application into the "out" folder (Mac / Linux).
set -e
cd "$(dirname "$0")"
rm -rf out
mkdir -p out
javac -d out $(find src/main -name "*.java")
echo "Build OK -> out/"
