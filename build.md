# insights-runtimes-inventory
Runtimes Based Inventory Service

```shell
mvn clean package // exclude integration tests
mvn clean package -P coverage // exclude integration tests + vanilla jacoco
mvn clean package -P integration // include all tests
mvn clean package -P integration-coverage // include all tests + quarkus-jacoco
```
