#!/bin/bash
# Compiles application + tests into "out-test" and runs the logic tests (Mac / Linux). No database needed.
set -e
cd "$(dirname "$0")"
rm -rf out-test
mkdir -p out-test
javac -d out-test $(find src/main src/test -name "*.java")
java -cp out-test com.parkomatic.TestRunner
