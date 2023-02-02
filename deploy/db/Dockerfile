####
# This Dockerfile is used in order to build a container that runs as an initcontainer
# when the Quarkus RBI service is started.
###

# Build the container
FROM registry.access.redhat.com/ubi8/ubi-minimal:latest
USER root

RUN microdnf install python \
    && microdnf update \
    && microdnf clean all
