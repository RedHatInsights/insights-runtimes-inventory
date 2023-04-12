#!/bin/bash

set -exv

REST_IMAGE_NAME="quay.io/cloudservices/insights-rbi-rest"
EVENTS_IMAGE_NAME="quay.io/cloudservices/insights-rbi-events"
IMAGE_TAG=$(git rev-parse --short=7 HEAD)

if [[ -z "$QUAY_USER" || -z "$QUAY_TOKEN" ]]; then
    echo "QUAY_USER and QUAY_TOKEN must be set"
    exit 1
fi

DOCKER_CONF="$PWD/.docker"
mkdir -p "$DOCKER_CONF"
docker --config="$DOCKER_CONF" login -u="$QUAY_USER" -p="$QUAY_TOKEN" quay.io
docker --config="$DOCKER_CONF" build -t "${REST_IMAGE_NAME}:${IMAGE_TAG}" -t "${REST_IMAGE_NAME}:latest" -f deploy/docker/rest/Dockerfile .
docker --config="$DOCKER_CONF" build -t "${EVENTS_IMAGE_NAME}:${IMAGE_TAG}" -t "${EVENTS_IMAGE_NAME}:latest" -f deploy/docker/events/Dockerfile .

docker --config="$DOCKER_CONF" push "${REST_IMAGE_NAME}:${IMAGE_TAG}"
docker --config="$DOCKER_CONF" push "${REST_IMAGE_NAME}:latest"
docker --config="$DOCKER_CONF" push "${EVENTS_IMAGE_NAME}:${IMAGE_TAG}"
docker --config="$DOCKER_CONF" push "${EVENTS_IMAGE_NAME}:latest"
