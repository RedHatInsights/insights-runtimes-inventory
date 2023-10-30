####
# This Dockerfile is used in order to run unit tests for the Jenkinsfile pipeline PR checks
###

# Build the project, and place coverage + unit test reports into a folder at /home/jboss/artifacts
FROM registry.access.redhat.com/ubi8/openjdk-17:latest
USER root
COPY . /home/jboss
WORKDIR /home/jboss
RUN ./mvnw clean package -P coverage --no-transfer-progress
RUN mkdir -p artifacts
RUN cp core/target/surefire-reports/TEST-*.xml artifacts
RUN cp events/target/surefire-reports/TEST-*.xml artifacts
RUN cp coverage/target/site/jacoco-aggregate/jacoco.xml artifacts
