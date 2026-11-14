#!/bin/bash
# Builds and starts Park-O-Matic (Mac / Linux). Put the JDBC driver jar in the "lib" folder first.
set -e
cd "$(dirname "$0")"
./build.sh
java -cp "out:lib/*" com.parkomatic.Main
