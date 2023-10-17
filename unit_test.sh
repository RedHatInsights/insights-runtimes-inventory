#!/bin/bash

# https://github.com/RedHatInsights/cicd-tools/blob/main/examples/unit_test_example.sh

# Run unit tests
./mvnw clean test --no-transfer-progress
result=$?

# Evaluate the test result.

# If your unit tests store junit xml results, you should store them in a file matching format `artifacts/junit-*.xml`
mkdir -p artifacts
for directory in "core" "events"; do # currently no "unit" test in /rest
    cp $directory/target/surefire-reports/TEST-*.xml artifacts
done

for file in $(ls artifacts/); do
    mv artifacts/$file artifacts/junit-$file
done

if [ $result -ne 0 ]; then
  echo '====================================='
  echo '====  ✖ ERROR: UNIT TEST FAILED  ===='
  echo '====================================='
  exit 1
fi
