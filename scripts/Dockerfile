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

FROM base as sonarqube-setup
# The argument that holds Red Hat IT's custom certificate's location.
ARG rh_it_root_ca_cert_url
RUN mkdir $PWD/sonarqube/
RUN mkdir $PWD/sonarqube/certs/
RUN mkdir $PWD/sonarqube/store/
RUN curl -o $PWD/sonarqube/certs/RH-IT-Root-CA.crt --insecure "${rh_it_root_ca_cert_url}"
RUN keytool \
    -keystore /$PWD/sonarqube/store/RH-IT-Root-CA.keystore \
    -import \
    -alias RH-IT-Root-CA \
    -file $PWD/sonarqube/certs/RH-IT-Root-CA.crt \
    -storepass redhat \
    -noprompt
RUN export SONAR_SCANNER_OPTS="-Djavax.net.ssl.trustStore=$PWD/sonarqube/store/RH-IT-Root-CA.keystore -Djavax.net.ssl.trustStorePassword=redhat"
# Set up local postgres for the QuarkusTests
RUN microdnf install -y dnf
RUN dnf install -y 'dnf-command(config-manager)'
RUN dnf install -y  https://download.postgresql.org/pub/repos/yum/reporpms/EL-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm
RUN dnf install -y postgresql15-server
RUN dnf install -y postgresql15-contrib
