# export TEMP_INSIGHTS_TOKEN=amRvZTpCbE1aaUFZZklGYWVSdzNH
# export TEMP_INSIGHTS_SERVER=https://env-ephemeral-0rn5hk-4kgktv87.apps.c-rh-c-eph.8p0c.p1.openshiftapps.com

#-H "x-rh-request_id: testtesttest"
curl -H "Authorization: Basic ${TEMP_INSIGHTS_TOKEN}" \
 "${TEMP_INSIGHTS_SERVER}/api/runtimes-inventory/v1/instance/?hostname=${HOSTNAME}" -v
