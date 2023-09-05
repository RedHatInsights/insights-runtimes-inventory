#!/bin/bash


REST_IMAGE_NAME="quay.io/$USER/insights-rbi-rest"
EVENTS_IMAGE_NAME="quay.io/$USER/insights-rbi-events"
IMAGE_TAG=$(git rev-parse --short=7 HEAD)

docker build -t "${REST_IMAGE_NAME}:${IMAGE_TAG}" -t "${REST_IMAGE_NAME}:latest" -f deploy/docker/rest/Dockerfile .
docker build -t "${EVENTS_IMAGE_NAME}:${IMAGE_TAG}" -t "${EVENTS_IMAGE_NAME}:latest" -f deploy/docker/events/Dockerfile .

docker push "${REST_IMAGE_NAME}:${IMAGE_TAG}"
docker push "${REST_IMAGE_NAME}:latest"
docker push "${EVENTS_IMAGE_NAME}:${IMAGE_TAG}"
docker push "${EVENTS_IMAGE_NAME}:latest"

