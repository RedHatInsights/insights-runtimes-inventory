# export TEMP_INSIGHTS_SERVER=https://env-ephemeral-zdazhn-urtnxyqd.apps.c-rh-c-eph.8p0c.p1.openshiftapps.com
# export TEMP_INSIGHTS_TOKEN=amRvZTpwZlFvUU5kc0xEc2NlaUU5

curl -H "Authorization: Basic ${TEMP_INSIGHTS_TOKEN}" \
 "${TEMP_INSIGHTS_SERVER}/api/runtimes-inventory/v1/instance/?hostname=${HOSTNAME}" -v
