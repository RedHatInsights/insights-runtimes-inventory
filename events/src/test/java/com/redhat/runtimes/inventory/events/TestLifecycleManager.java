/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.events;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.config.ConfigProvider;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * This class is based on TestLifecycleManager from RedHatInsights/notifications-backend:
 * https://github.com/RedHatInsights/notifications-backend/blob/master/engine/src/test/java/com/redhat/cloud/notifications/TestLifecycleManager.java
 */
public class TestLifecycleManager implements QuarkusTestResourceLifecycleManager {

  Boolean quarkusDevServiceEnabled = true;

  PostgreSQLContainer<?> postgreSQLContainer;

  @Override
  public Map<String, String> start() {
    Optional<Boolean> quarkusDevServiceEnabledFlag =
        ConfigProvider.getConfig().getOptionalValue("quarkus.devservices.enabled", Boolean.class);
    if (quarkusDevServiceEnabledFlag.isPresent()) {
      quarkusDevServiceEnabled = quarkusDevServiceEnabledFlag.get();
    }

    Map<String, String> properties = new HashMap<>();
    try {
      if (quarkusDevServiceEnabled) {
        setupPostgres(properties);
      }
      setupInMemoryConnector(properties);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return properties;
  }

  private void setupInMemoryConnector(Map<String, String> props) {
    /*
     * We'll use an in-memory Reactive Messaging connector to send payloads.
     * See https://smallrye.io/smallrye-reactive-messaging/smallrye-reactive-messaging/2/testing/testing.html
     */
    props.putAll(
        InMemoryConnector.switchIncomingChannelsToInMemory(
            com.redhat.runtimes.inventory.events.EventConsumer.INGRESS_CHANNEL));
    props.putAll(
        InMemoryConnector.switchIncomingChannelsToInMemory(
            com.redhat.runtimes.inventory.events.EventConsumer.EGG_CHANNEL));
  }

  @Override
  public void stop() {
    postgreSQLContainer.stop();
    InMemoryConnector.clear();
  }

  void setupPostgres(Map<String, String> props) throws SQLException {
    postgreSQLContainer = new PostgreSQLContainer<>("postgres:15");
    postgreSQLContainer.start();
    String jdbcUrl = postgreSQLContainer.getJdbcUrl();
    props.put("quarkus.datasource.jdbc.url", jdbcUrl);
    props.put("quarkus.datasource.username", "test");
    props.put("quarkus.datasource.password", "test");
    props.put("quarkus.datasource.db-kind", "postgresql");
  }
}
