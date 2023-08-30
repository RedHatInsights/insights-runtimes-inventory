/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.events;

import static com.redhat.runtimes.inventory.events.EventConsumer.CONSUMED_TIMER_NAME;
import static com.redhat.runtimes.inventory.events.EventConsumer.INGRESS_CHANNEL;
import static com.redhat.runtimes.inventory.events.EventConsumer.PROCESSING_EXCEPTION_COUNTER_NAME;
import static com.redhat.runtimes.inventory.events.TestUtils.readBytesFromResources;
import static com.redhat.runtimes.inventory.events.TestUtils.readFromResources;
import static com.redhat.runtimes.inventory.events.Utils.jvmInstanceOf;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.redhat.runtimes.inventory.models.InsightsMessage;
import com.redhat.runtimes.inventory.models.JvmInstance;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
@Tag("integration-tests")
public class EventConsumerIntegrationTest {

  @Inject EntityManager entityManager;

  @Inject @Any InMemoryConnector inMemoryConnector;

  @Inject MicrometerAssertionHelper micrometerAssertionHelper;

  @BeforeEach
  void beforeEach() {
    micrometerAssertionHelper.saveCounterValuesBeforeTest(PROCESSING_EXCEPTION_COUNTER_NAME);
    micrometerAssertionHelper.removeDynamicTimer(CONSUMED_TIMER_NAME);
  }

  @AfterEach
  void clear() {
    micrometerAssertionHelper.clearSavedValues();
    micrometerAssertionHelper.removeDynamicTimer(CONSUMED_TIMER_NAME);
  }

  @Test
  @Transactional
  @SuppressWarnings("unchecked")
  void testValidJvmInstancePayload() throws IOException, InterruptedException {
    clearTables();
    HttpClient mockClient = mock(HttpClient.class);
    HttpResponse<byte[]> mockResponse = mock(HttpResponse.class);
    byte[] buffy = readBytesFromResources("jdk8_MWTELE-66.gz");
    when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);
    when(mockResponse.body()).thenReturn(buffy);

    EventConsumer.setHttpClient(mockClient);
    String kafkaMessage = readFromResources("incoming_kafka1.json");
    inMemoryConnector.source(INGRESS_CHANNEL).send(kafkaMessage);

    micrometerAssertionHelper.awaitAndAssertTimerIncrement(CONSUMED_TIMER_NAME, 1);
    micrometerAssertionHelper.assertCounterIncrement(PROCESSING_EXCEPTION_COUNTER_NAME, 0);
    assertEquals(1L, jvminstance_count());
  }

  @Test
  void testInvalidPayload() throws IOException {
    inMemoryConnector.source(INGRESS_CHANNEL).send("not a real payload");
    micrometerAssertionHelper.awaitAndAssertTimerIncrement(CONSUMED_TIMER_NAME, 1);
    micrometerAssertionHelper.assertCounterIncrement(PROCESSING_EXCEPTION_COUNTER_NAME, 1);
  }

  @Test
  @Transactional
  void testBasicPostgresTransactions() throws IOException {
    clearTables();
    ArchiveAnnouncement dummy = new ArchiveAnnouncement();
    dummy.setAccountId("dummy account id");
    dummy.setOrgId("dummy org");
    dummy.setTimestamp(Instant.now());

    byte[] buffy = readBytesFromResources("jdk8_MWTELE-66.gz");
    String json = EventConsumer.unzipJson(buffy);

    InsightsMessage msg = jvmInstanceOf(dummy, json);
    assertTrue(msg instanceof JvmInstance);
    JvmInstance inst = (JvmInstance) msg;

    assertEquals(0L, jvminstance_count());
    assertEquals(0L, jarhash_count());

    entityManager.persist(inst);
    assertEquals(1L, jvminstance_count());
    assertEquals(1L, jarhash_count());

    entityManager.remove(inst);
    assertEquals(0L, jvminstance_count());
    assertEquals(0L, jarhash_count());
  }

  private void clearTables() {
    entityManager.createNativeQuery("DELETE FROM jar_hash").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM jvm_instance").executeUpdate();
  }

  private Long jvminstance_count() {
    return entityManager
        .createQuery("SELECT COUNT (*) FROM JvmInstance", Long.class)
        .getSingleResult();
  }

  private Long jarhash_count() {
    return entityManager.createQuery("SELECT COUNT (*) FROM JarHash", Long.class).getSingleResult();
  }
}
