####
# This Dockerfile is used in order to build a container that runs as an initcontainer
# when the Quarkus RBI service is started.
###

# Build the container
FROM registry.access.redhat.com/ubi8/ubi-minimal:latest
USER root

RUN microdnf install python3 python3-devel \
    && microdnf install postgresql-devel gcc \
    && microdnf update \
    && microdnf clean all

RUN pip3 install psycopg2

RUN mkdir /app
COPY create_db.py /app
COPY ../../core/src/main/resources/migration/V1.0.0__runtime_instance.sql /app
WORKDIR /app

# ENTRYPOINT ["python3", "create_db.py"]
