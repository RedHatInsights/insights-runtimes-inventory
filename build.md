# insights-runtimes-inventory
Runtimes Based Inventory Service

## Test Coverage

There is a Maven profile ("`coverage`") that can be used to enable JaCoCo code coverage reports. This can be performed using both `mvn` and `mvnw`.

The resulting reports can be found in the coverage module. e.g., `/coverage/target/site/index.html`

```shell
mvn clean package // unit tests
mvn clean verify  // unit tests + integration (Quarkus) tests
mvn clean package -P coverage // unit tests + coverage report
mvn clean verify -P coverage // all tests + coverage report

./mvnw clean package // unit tests
./mvnw clean verify  // unit tests + integration (Quarkus) tests
./mvnw clean package -P coverage // unit tests + coverage report
./mvnw clean verify -P coverage // all tests + coverage report
```
