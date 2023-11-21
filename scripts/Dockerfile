####
# This Dockerfile is used in order to run tests & static analysis for the Jenkinsfile pipeline PR checks
###

FROM registry.access.redhat.com/ubi8/openjdk-17 as base
USER root
COPY . /home/jboss
WORKDIR /home/jboss

FROM base as unit-test
RUN ./mvnw clean package -P coverage --no-transfer-progress
RUN mkdir -p artifacts
RUN cp core/target/surefire-reports/TEST-*.xml artifacts

FROM base as integration-test-setup
RUN microdnf install -y dnf
RUN dnf install -y 'dnf-command(config-manager)'
RUN dnf install -y  https://download.postgresql.org/pub/repos/yum/reporpms/EL-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm
RUN dnf install -y postgresql15-server
RUN dnf install -y postgresql15-contrib
