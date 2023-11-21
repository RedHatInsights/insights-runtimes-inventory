#!/bin/bash

# Start Postgresql and init runtimes database
su -l postgres -c /usr/pgsql-15/bin/initdb
su -l postgres -c "/usr/pgsql-15/bin/pg_ctl -D /var/lib/pgsql/15/data -l /tmp/pg_logfile start"
createdb -U postgres runtimes

./mvnw clean verify -P coverage sonar:sonar \
    -Dsonar.host.url="${SONARQUBE_HOST_URL}" \
    -Dsonar.exclusions="**/*.sql" \
    -Dsonar.projectKey="com.redhat.insights:runtimes-inventory" \
    -Dsonar.projectVersion="${COMMIT_SHORT}" \
    -Dsonar.pullrequest.base="main" \
    -Dsonar.pullrequest.branch="${GIT_BRANCH}" \
    -Dsonar.pullrequest.key="${GITHUB_PULL_REQUEST_ID}" \
    -Dsonar.sourceEncoding="UTF-8" \
    -Dsonar.token="${SONARQUBE_TOKEN}" \
    -Dquarkus.devservices.enabled=false \
    --no-transfer-progress
