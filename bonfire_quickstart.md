# Bonfire Quickstart for Ephemeral Environments

Ensure that you're logged into the VPN.

Make sure that you have `$VENV_DIR` set (for a Python virtual environment for Bonfire).
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

This will give you an oc login command. Run it in your venv.
You are now credentialed in this shell for the ephemeral cluster.

You may need to set up DNS routing for a few domains to go via the VPN:

```
$ sudo systemd-resolve -i tun0 --set-domain=redhat.com --set-domain=~amazonaws.com --set-domain=~devshift.net --set-domain=openshiftapps.com
```

Enter the following command to deploy the advisor backend and frontend components along with their dependencies (notice the use of the `--frontends=true` argument):

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

You'll need to use the URL and login credentials listed in the command output to view your inventory, advisor and rbac apps in the web UI.

In your web browser, enter the console URL and keep it open. Check that the runtimes components have started OK (under Workloads -> Pods).

In your terminal window use the following command to generate a Basic auth header, if you need one:

```
TEMP_INSIGHTS_TOKEN=$(oc get secret env-$NAMESPACE-keycloak -n $NAMESPACE -o json | jq '.data | map_values(@base64d)' | jq -r -j '"\(.defaultUsername):\(.defaultPassword)" | @base64')
```

### Updating an Ephemeral env with local changes

(1-Off Task) Download K8s secret (if you don't have it already).

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

(Daily) Add the pullsecret to the env (needed, b/c our containers need to be pulled from our quay.io repo).
// This step is needed b/c the clowdservices secret doesn't know about our service.

```
oc apply -n $NAMESPACE -f beevans-secret.yml
```

(Daily)
Add to config (via oc edit env or the Clowder > ClowdEnvironments detail tab in the web console - i.e. env-ephemeral-XXXX):

```
	pullSecrets:
	    - name: quay-cloudservices-pull
	      namespace: ephemeral-base
      - name: beevans-pull-secret
        namespace: ephemeral-XXXXXX
```

// FIXME I think this is dead now
(Daily) Edit the Ingress Clowdapp to tell it about our Kafka topics (under the existing ones)

```
    - partitions: 3
      replicas: 3
      topicName: platform.upload.runtimes-java-general
```

(Every Push) Update the clowdapp YAML (e.g. clowdapp-runtimes-minimal.yml) to use the new tag and todays namespace.

Then deploy the clowdapp:

```
oc apply -n $NAMESPACE -f clowdapp-runtimes-minimal.yml
```

### Redeploy

Update the YAML then

```
oc apply -n $NAMESPACE -f clowdapp-runtimes-minimal.yml
```

### Adding to stage

Merge to `main`
