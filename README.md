# insights-runtimes-inventory
Runtimes Based Inventory Service


### Adding to Ephemeral env

Build a container and use quay.io to push container:

docker build -t quay.io/beevans/runtimes-inventory:rcN .
docker push quay.io/beevans/runtimes-inventory:rcN

(1-Off Task) Download K8s secret (if you don't have it already) & then:

```
oc apply -n $NAMESPACE -f beevans-secret.yml
```

Update the clowdapp YAML (e.g. clowdapp-runtimes-minimal.yml) to use the new RC version and todays namespace.

Then deploy the clowdapp:

```
oc apply -n $NAMESPACE -f clowdapp-runtimes-minimal.yml
```

Add to config (via oc edit env or the Clowd > ClowdEnvironments tab in the web console):

```
	pullSecrets:
	- name: quay-cloudservices-pull
	  namespace: ephemeral-base
	- name: beevans-pull-secret
	  namespace: ephemeral-< NAMESPACE >
```

This step is needed b/c the clowdservices secret doesn't know about our service (yet!).

(Daily Task) Tell Ingress about our Kafka topics (under the existing ones)

	- partitions: 3
    replicas: 3
	  topicName: platform.upload.runtimes-java-general

### Redeploy

Update the YAML then

```
oc apply -n $NAMESPACE -f clowdapp-runtimes-minimal.yml
```
