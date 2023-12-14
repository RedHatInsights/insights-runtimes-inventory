#!/bin/sh

# Append any Cryostat-specific options to JAVA_OPTS_APPEND
if [ -n "${CRYOSTAT_JAVA_OPTS}" ]; then
  # Java requires the JMX credentials file to be readable only by the
  # JVM process owner. Because OpenShift uses arbitrary UIDs when running
  # containers, we need to copy the file to change ownership and then
  # reduce its permissions.
  if [ -n "${CRYOSTAT_JMX_PASSWORD_FILE}" ] && [ -n "${CRYOSTAT_JMX_ACCESS_FILE}" ]; then
      tmpdir="$(mktemp -d)"
      passwordFile="${tmpdir}/jmxremote.password"
      cp "${CRYOSTAT_JMX_PASSWORD_FILE}" "${passwordFile}"
      chmod 400 "${passwordFile}"
      accessFile="${tmpdir}/jmxremote.access"
      cp "${CRYOSTAT_JMX_ACCESS_FILE}" "${accessFile}"
      chmod 400 "${accessFile}"
      CRYOSTAT_JAVA_OPTS="${CRYOSTAT_JAVA_OPTS} \
        -Dcom.sun.management.jmxremote.password.file=${passwordFile} \
        -Dcom.sun.management.jmxremote.access.file=${accessFile}"
  fi

  export JAVA_OPTS_APPEND="${JAVA_OPTS_APPEND} ${CRYOSTAT_JAVA_OPTS}"
fi
