/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.events;

import static com.redhat.runtimes.inventory.events.EventConsumer.CONSUMED_TIMER_NAME;
import static com.redhat.runtimes.inventory.events.EventConsumer.INGRESS_CHANNEL;
import static com.redhat.runtimes.inventory.events.EventConsumer.PROCESSING_EXCEPTION_COUNTER_NAME;
import static com.redhat.runtimes.inventory.events.TestUtils.readBytesFromResources;
import static com.redhat.runtimes.inventory.events.TestUtils.readFromResources;
import static com.redhat.runtimes.inventory.events.Utils.eapInstanceOf;
import static com.redhat.runtimes.inventory.events.Utils.instanceOf;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.redhat.runtimes.inventory.models.EapInstance;
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
    TestUtils.clearTables(entityManager);
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
    assertEquals(1L, TestUtils.entity_count(entityManager, "JvmInstance"));
  }

  @Test
  void testInvalidPayload() throws IOException {
    inMemoryConnector.source(INGRESS_CHANNEL).send("not a real payload");
    micrometerAssertionHelper.awaitAndAssertTimerIncrement(CONSUMED_TIMER_NAME, 1);
    micrometerAssertionHelper.assertCounterIncrement(PROCESSING_EXCEPTION_COUNTER_NAME, 1);
  }

  @Test
  @Transactional
  void testJvmInstanceBasicPostgresTransactions() throws IOException {
    TestUtils.clearTables(entityManager);
    ArchiveAnnouncement dummy = new ArchiveAnnouncement();
    dummy.setAccountId("dummy account id");
    dummy.setOrgId("dummy org");
    dummy.setTimestamp(Instant.now());

    byte[] buffy = readBytesFromResources("jdk8_MWTELE-66.gz");
    String json = EventConsumer.unzipJson(buffy);

    InsightsMessage msg = instanceOf(dummy, json);
    assertTrue(msg instanceof JvmInstance);
    JvmInstance inst = (JvmInstance) msg;

    assertEquals(0L, TestUtils.entity_count(entityManager, "JvmInstance"));
    assertEquals(0L, TestUtils.entity_count(entityManager, "JarHash"));

    entityManager.persist(inst);
    assertEquals(1L, TestUtils.entity_count(entityManager, "JvmInstance"));
    assertEquals(1L, TestUtils.entity_count(entityManager, "JarHash"));
    assertEquals(1L, TestUtils.table_count(entityManager, "jvm_instance_jar_hash"));

    entityManager.remove(inst);
    assertEquals(0L, TestUtils.entity_count(entityManager, "JvmInstance"));
    // TODO: This won't pass right now because orphans still need to be handled
    // assertEquals(0L, TestUtils.entity_count(entityManager, "JarHash"));
    assertEquals(0L, TestUtils.table_count(entityManager, "jvm_instance_jar_hash"));
  }

  @Test
  @Transactional
  void testEapInstanceBasicPostgresTransactions() throws IOException {
    TestUtils.clearTables(entityManager);
    ArchiveAnnouncement dummy = new ArchiveAnnouncement();
    dummy.setAccountId("dummy account id");
    dummy.setOrgId("dummy org");
    dummy.setTimestamp(Instant.now());

    String json = readFromResources("eap_example1.json");

    InsightsMessage msg = eapInstanceOf(dummy, json);
    assertTrue(msg instanceof EapInstance);
    EapInstance inst = (EapInstance) msg;

    /*******************
     *  Pre-persist checks
     *******************/
    assertEquals(0L, TestUtils.entity_count(entityManager, "EapInstance"));
    assertEquals(0L, TestUtils.entity_count(entityManager, "JarHash"));
    assertEquals(0L, TestUtils.entity_count(entityManager, "EapConfiguration"));
    assertEquals(0L, TestUtils.entity_count(entityManager, "EapDeployment"));
    assertEquals(0L, TestUtils.entity_count(entityManager, "EapExtension"));

    /*******************
     *  Persist and check counts
     *******************/
    entityManager.persist(inst);
    assertEquals(1L, TestUtils.entity_count(entityManager, "EapInstance"));
    assertEquals(1L, TestUtils.entity_count(entityManager, "EapConfiguration"));
    assertEquals(2L, TestUtils.table_count(entityManager, "eap_configuration_deployments"));
    assertEquals(40L, TestUtils.table_count(entityManager, "eap_configuration_subsystems"));
    assertEquals(2L, TestUtils.entity_count(entityManager, "EapDeployment"));
    assertEquals(39L, TestUtils.entity_count(entityManager, "EapExtension"));
    assertEquals(41L, TestUtils.table_count(entityManager, "eap_extension_subsystems"));

    // Jars are stored in multiple places, so lots of JarHashes
    assertEquals(3561L, TestUtils.entity_count(entityManager, "JarHash"));
    assertEquals(1L, TestUtils.table_count(entityManager, "jvm_instance_jar_hash"));
    assertEquals(3555L, TestUtils.table_count(entityManager, "eap_instance_module_jar_hash"));
    assertEquals(5L, TestUtils.table_count(entityManager, "eap_deployment_archive_jar_hash"));

    // The JarHash entities should equal the totals in these tables
    assertEquals(
        TestUtils.entity_count(entityManager, "JarHash"),
        TestUtils.table_count(entityManager, "jvm_instance_jar_hash")
            + TestUtils.table_count(entityManager, "eap_instance_module_jar_hash")
            + TestUtils.table_count(entityManager, "eap_deployment_archive_jar_hash"));

    /*******************
     *  Instance removal checks
     *******************/
    entityManager.remove(inst);
    assertEquals(0L, TestUtils.entity_count(entityManager, "EapInstance"));
    assertEquals(0L, TestUtils.entity_count(entityManager, "EapConfiguration"));
    assertEquals(0L, TestUtils.table_count(entityManager, "eap_configuration_deployments"));
    assertEquals(0L, TestUtils.table_count(entityManager, "eap_configuration_subsystems"));
    assertEquals(0L, TestUtils.entity_count(entityManager, "EapDeployment"));

    // TODO: These won't pass right now because orphans still need to be handled
    // assertEquals(0L, TestUtils.entity_count(entityManager, "EapExtension"));
    // assertEquals(0L, TestUtils.table_count(entityManager, "eap_extension_subsystems"));
    // assertEquals(0L, TestUtils.entity_count(entityManager, "JarHash"));
    assertEquals(0L, TestUtils.table_count(entityManager, "jvm_instance_jar_hash"));
    assertEquals(0L, TestUtils.table_count(entityManager, "eap_instance_module_jar_hash"));
    assertEquals(0L, TestUtils.table_count(entityManager, "eap_deployment_archive_jar_hash"));
  }
}
