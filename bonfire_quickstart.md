# Bonfire Quickstart for Ephemeral Environments

Ensure that you're logged into the VPN.

Make sure that you have `$VENV_DIR` set (for a Python virtual environment "venv" for Bonfire).
You can add the env variable to your shell profile file.

```
VENV_DIR=~/bonfire_venv
```

Activate the environment:

```
. $VENV_DIR/bin/activate
```

You may need to log in to the ephemeral cluster, by using this page (uses Github to authenticate):

https://oauth-openshift.apps.c-rh-c-eph.8p0c.p1.openshiftapps.com/oauth/token/request

This will give you an `oc login` command. Run it in your venv.

You are now credentialed in this shell for the ephemeral cluster.
You may well need more than one credentialed shell to work effectively.

You may need to set up DNS routing for a few domains to go via the VPN:

```
$ sudo systemd-resolve -i tun0 --set-domain=redhat.com --set-domain=~amazonaws.com --set-domain=~devshift.net --set-domain=openshiftapps.com
```

If you are running with overrides (e.g. a locally-built container with modified code), then you will need to make sure that the containers have been built and push to quay.io first:

```
$ ./build_ee.sh
```

Enter the following command to deploy the runtimes-inventory backend and frontend components along with their dependencies (notice the use of the `--frontends=true` argument):

This command requires you to be on the VPN and will fail with a cryptic error message if you're not connected.

```
$ NAMESPACE=$(bonfire deploy runtimes-inventory --frontends=true)
```

You may also deploy "advisor" for a fuller set of components

Next, extend your Ephemeral Environment for the time left in your working day:

```
$ bonfire namespace extend $NAMESPACE --duration 12h
```

Enter the following command to print access info for your namespace

```
$ bonfire namespace describe $NAMESPACE
```

NOTE: `$NAMESPACE` is optional for quite a few commands, but you may find it helpful to be explicit about its usage, especially in complex setups.

You'll need to use the URL and login credentials listed in the command output to view your inventory, advisor and rbac apps in the web UI.

In your web browser, enter the console URL and keep it open. Check that the runtimes components have started OK (under Workloads -> Pods).

In your terminal window use the following command to generate a Basic auth header, if you need one:

```
RHT_INSIGHTS_JAVA_AUTH_TOKEN=$(oc get secret env-$NAMESPACE-keycloak -n $NAMESPACE -o json | jq '.data | map_values(@base64d)' | jq -r -j '"\(.defaultUsername):\(.defaultPassword)" | @base64')
```

### Updating an Ephemeral env with local changes

Build a container and push it to quay.io:

```
REST_IMAGE_NAME="quay.io/$USER/insights-rbi-rest"
EVENTS_IMAGE_NAME="quay.io/$USER/insights-rbi-events"
IMAGE_TAG=$(git rev-parse --short=7 HEAD)

docker build -t "${REST_IMAGE_NAME}:${IMAGE_TAG}" -t "${REST_IMAGE_NAME}:latest" -f deploy/docker/rest/Dockerfile .
docker build -t "${EVENTS_IMAGE_NAME}:${IMAGE_TAG}" -t "${EVENTS_IMAGE_NAME}:latest" -f deploy/docker/events/Dockerfile .

docker push "${REST_IMAGE_NAME}:${IMAGE_TAG}"
docker push "${REST_IMAGE_NAME}:latest"
docker push "${EVENTS_IMAGE_NAME}:${IMAGE_TAG}"
docker push "${EVENTS_IMAGE_NAME}:latest"
```

To deploy from our personal quay.io repos, we need to configure `~/.config/bonfire/config.yaml`.

1. This provides overrides to the base config
2. In particular, it allows a redirect to a different Clowder deployment file.
3. The variant deployment file can point at a different quay.io repo

NOTE: The repos we want to deploy from must be public. Previous versions of instructions involving private repos and pullsecrets will no longer work.

A starter version of the config (for a user that keeps their projects under `~/projects/`) looks like this:

```
# Bonfire deployment configuration

# Defines where to fetch the file that defines application configs
appsFile:
  host: gitlab
  repo: insights-platform/cicd-common
  path: bonfire_configs/ephemeral_apps.yaml

# (optional) define any apps locally. An app defined here with <name> will override config for app
# <name> in above fetched config.

apps:
- name: runtimes-inventory
  components:
    - name: runtimes-inventory
      host: local
      repo: ~/projects/insights-runtimes-inventory
      path: deploy/clowdapp.yml
      parameters:
        IMAGE_NAMESPACE: <your quay.io username>
        IMAGE_TAG: latest
```

Note that the app and component name must be `runtimes-inventory`.

/////////////////////

