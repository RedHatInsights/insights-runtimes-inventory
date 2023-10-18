#!/bin/bash

# Run unit tests
DOCKER_CONF="$PWD/.docker"
docker --config="$DOCKER_CONF" build -t insights-runtimes-inventory/unit-test:latest -f scripts/Dockerfile .
result=$?

if [ $result -eq 0 ]; then
    # Retrieve the results of the unit tests from a container
    id=$(docker --config="$DOCKER_CONF" create insights-runtimes-inventory/unit-test)
    docker --config="$DOCKER_CONF" cp $id:/home/jboss/artifacts ./
    docker --config="$DOCKER_CONF" rm -v $id

    # If your unit tests store junit xml results, you should store them in a file matching format `artifacts/junit-*.xml`
    for report in $(ls artifacts); do
        if [[ $report == TEST* ]]; then
            mv artifacts/$report artifacts/junit-$report;
        fi
    done
else
    echo '====================================='
    echo '====  ✖ ERROR: UNIT TEST FAILED  ===='
    echo '====================================='
    exit 1
fi
