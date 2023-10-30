#!/bin/sh

if [ -n "${CRYOSTAT_JAVA_OPTS}" ]; then
  export JAVA_OPTS_APPEND="${JAVA_OPTS_APPEND} ${CRYOSTAT_JAVA_OPTS}"
fi
