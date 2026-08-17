#!/usr/bin/env sh
set -eu

rm -rf build
mkdir -p build/main build/test
javac -d build/main $(find src/main/java -name '*.java')
javac -cp build/main -d build/test $(find src/test/java -name '*.java')
java -cp build/main:build/test org.example.nonprofit.service.WeeklyDigestServiceTest

if [ "${1:-}" = "schedule" ]; then
  java -cp build/main org.example.nonprofit.DigestScheduler
fi
