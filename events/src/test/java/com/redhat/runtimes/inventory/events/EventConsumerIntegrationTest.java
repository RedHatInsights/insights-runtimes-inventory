/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.events;

import static com.redhat.runtimes.inventory.events.EventConsumer.CONSUMED_TIMER_NAME;
import static com.redhat.runtimes.inventory.events.EventConsumer.INGRESS_CHANNEL;
import static com.redhat.runtimes.inventory.events.EventConsumer.PROCESSING_EXCEPTION_COUNTER_NAME;
import static com.redhat.runtimes.inventory.events.TestUtils.readBytesFromResources;
import static com.redhat.runtimes.inventory.events.TestUtils.readFromResources;
import static com.redhat.runtimes.inventory.events.Utils.eapInstanceOf;
import static com.redhat.runtimes.inventory.events.Utils.jvmInstanceOf;
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
    assertEquals(1L, entity_count("JvmInstance"));
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

    assertEquals(0L, entity_count("JvmInstance"));
    assertEquals(0L, entity_count("JarHash"));

    entityManager.persist(inst);
    assertEquals(1L, entity_count("JvmInstance"));
    assertEquals(1L, entity_count("JarHash"));
    assertEquals(1L, table_count("jvm_instance_jar_hash"));

    entityManager.remove(inst);
    assertEquals(0L, entity_count("JvmInstance"));
    // TODO: This won't pass right now because orphans still need to be handled
    // assertEquals(0L, entity_count("JarHash"));
    assertEquals(0L, table_count("jvm_instance_jar_hash"));
  }

  @Test
  @Transactional
  void testEapInstanceBasicPostgresTransactions() throws IOException {
    clearTables();
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
    assertEquals(0L, entity_count("EapInstance"));
    assertEquals(0L, entity_count("JarHash"));
    assertEquals(0L, entity_count("EapConfiguration"));
    assertEquals(0L, entity_count("EapDeployment"));
    assertEquals(0L, entity_count("EapExtension"));

    /*******************
     *  Persist and check counts
     *******************/
    entityManager.persist(inst);
    assertEquals(1L, entity_count("EapInstance"));
    assertEquals(1L, entity_count("EapConfiguration"));
    assertEquals(2L, table_count("eap_configuration_deployments"));
    assertEquals(40L, table_count("eap_configuration_subsystems"));
    assertEquals(2L, entity_count("EapDeployment"));
    assertEquals(39L, entity_count("EapExtension"));
    assertEquals(41L, table_count("eap_extension_subsystems"));

    // Jars are stored in multiple places, so lots of JarHashes
    assertEquals(3561L, entity_count("JarHash"));
    assertEquals(1L, table_count("jvm_instance_jar_hash"));
    assertEquals(3555L, table_count("eap_instance_module_jar_hash"));
    assertEquals(5L, table_count("eap_deployment_archive_jar_hash"));

    // The JarHash entities should equal the totals in these tables
    assertEquals(
        entity_count("JarHash"),
        table_count("jvm_instance_jar_hash")
            + table_count("eap_instance_module_jar_hash")
            + table_count("eap_deployment_archive_jar_hash"));

    /*******************
     *  Instance removal checks
     *******************/
    entityManager.remove(inst);
    assertEquals(0L, entity_count("EapInstance"));
    assertEquals(0L, entity_count("EapConfiguration"));
    assertEquals(0L, table_count("eap_configuration_deployments"));
    assertEquals(0L, table_count("eap_configuration_subsystems"));
    assertEquals(0L, entity_count("EapDeployment"));

    // TODO: These won't pass right now because orphans still need to be handled
    // assertEquals(0L, entity_count("EapExtension"));
    // assertEquals(0L, table_count("eap_extension_subsystems"));
    // assertEquals(0L, entity_count("JarHash"));
    assertEquals(0L, table_count("jvm_instance_jar_hash"));
    assertEquals(0L, table_count("eap_instance_module_jar_hash"));
    assertEquals(0L, table_count("eap_deployment_archive_jar_hash"));
  }

  private void clearTables() {
    // Order is important here
    entityManager.createNativeQuery("DELETE FROM jvm_instance_jar_hash").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM eap_instance_module_jar_hash").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM eap_deployment_archive_jar_hash").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM jar_hash").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM jvm_instance").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM eap_instance").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM eap_configuration").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM eap_configuration_eap_extension").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM eap_configuration_deployments").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM eap_configuration_subsystems").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM eap_deployment").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM eap_extension").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM eap_extension_subsystems").executeUpdate();
  }

  private Long entity_count(String entity) {
    // I don't know why, but but hibernate throws a ParsingException
    // when I try a named or positional query parameter
    return entityManager
        .createQuery("SELECT COUNT (*) FROM " + entity, Long.class)
        .getSingleResult();
  }

  private Long table_count(String table) {
    // I don't know why, but but hibernate throws a ParsingException
    // when I try a named or positional query parameter
    return (Long)
        entityManager
            .createNativeQuery("SELECT COUNT (*) FROM " + table, Long.class)
            .getSingleResult();
  }
}
