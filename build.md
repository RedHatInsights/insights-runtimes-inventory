# insights-runtimes-inventory
Runtimes Based Inventory Service

```shell
./mvnw clean package -DskipTests --no-transfer-progress
```

### Adding to Ephemeral env

(General)
Build a container and use quay.io to push container:

docker build -t quay.io/beevans/runtimes-inventory:rcN .
docker push quay.io/beevans/runtimes-inventory:rcN

(1-Off Task) Download K8s secret (if you don't have it already).

(Daily) Add the pullsecret to the env (needed, b/c our app is not fully integrated into the env yet).
This step is needed b/c the clowdservices secret doesn't know about our service (yet!).

```
oc apply -n $NAMESPACE -f beevans-secret.yml
```

(Daily)
Add to config (via oc edit env or the Clowd > ClowdEnvironments detail tab in the web console - i.e. env-ephemeral-XXXX):

```
	pullSecrets:
	    - name: quay-cloudservices-pull
	      namespace: ephemeral-base
      - name: beevans-pull-secret
        namespace: ephemeral-XXXXXX
```

(Daily) Edit the Ingress Clowdapp to tell it about our Kafka topics (under the existing ones)

```
    - partitions: 3
      replicas: 3
      topicName: platform.upload.runtimes-java-general
```

(Every Push) Update the clowdapp YAML (e.g. clowdapp-runtimes-minimal.yml) to use the new RC version and todays namespace.

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

```
docker build -t quay.io/cloudservices/insights-runtimes-inventory .
docker push quay.io/cloudservices/insights-runtimes-inventory
```

