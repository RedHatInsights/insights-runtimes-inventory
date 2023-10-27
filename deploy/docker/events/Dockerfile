####
# This Dockerfile is used in order to build a container that runs the RBI Events Quarkus application in JVM mode
###

# Build the project
FROM registry.access.redhat.com/ubi8/openjdk-17:latest AS build
USER root
COPY . /home/jboss
WORKDIR /home/jboss
RUN ./mvnw clean package -DskipTests --no-transfer-progress

# Build the container
FROM registry.access.redhat.com/ubi8/openjdk-17-runtime:latest

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'

# Use four distinct layers so if there are application changes the library layers can be re-used
COPY --from=build --chown=185 /home/jboss/events/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build --chown=185 /home/jboss/events/target/quarkus-app/*.jar /deployments/
COPY --from=build --chown=185 /home/jboss/events/target/quarkus-app/app/ /deployments/app/
COPY --from=build --chown=185 /home/jboss/events/target/quarkus-app/quarkus/ /deployments/quarkus/

# Add a custom run-env.sh script to append any Cryostat system properties
COPY --from=build --chown=185 /home/jboss/deploy/docker/common/cryostat-run-env.sh /deployments/run-env.sh

ENV JAVA_OPTS_APPEND="-XX:-ExitOnOutOfMemoryError -Dquarkus.http.host=0.0.0.0 -Dquarkus.http.port=8000 -Djava.util.logging.manager=org.jboss.logmanager.LogManager -XX:+PrintFlagsFinal"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

EXPOSE 8000
USER 185
