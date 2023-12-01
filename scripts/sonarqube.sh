#!/bin/bash

DOCKER_CONF="$PWD/.docker"

#
# Get the commit SHA to give the scanner a unique "project version" setting.
#
readonly COMMIT_SHORT=$(git rev-parse --short=7 HEAD)

# Build the docker image
docker --config="$DOCKER_CONF" build \
  --file scripts/Dockerfile \
  --target integration-test-setup \
  --tag runtimes-inventory-sonarqube:latest \
  .

# Run the scan code script on the container
docker --config="$DOCKER_CONF" run \
  --env COMMIT_SHORT="${COMMIT_SHORT}" \
  --env GIT_BRANCH="${GIT_BRANCH}" \
  --env GITHUB_PULL_REQUEST_ID="${ghprbPullId}" \
  --env SONARQUBE_HOST_URL="${SONARQUBE_HOST_URL}" \
  --env SONARQUBE_TOKEN="${SONARQUBE_TOKEN}" \
  --rm \
  runtimes-inventory-sonarqube:latest \
  bash scripts/scan_code.sh
