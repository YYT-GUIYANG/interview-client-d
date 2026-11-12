#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build/classes
javac -encoding UTF-8 -d build/classes src/main/java/edu/demo/interview/*.java src/test/java/edu/demo/interview/*.java
java -cp build/classes edu.demo.interview.CoreCheck
