/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.events;

import static com.redhat.runtimes.inventory.events.EventConsumer.CONSUMED_TIMER_NAME;
import static com.redhat.runtimes.inventory.events.EventConsumer.INGRESS_CHANNEL;
import static com.redhat.runtimes.inventory.events.EventConsumer.PROCESSING_EXCEPTION_COUNTER_NAME;
import static com.redhat.runtimes.inventory.events.EventConsumer.eapInstanceOf;
import static com.redhat.runtimes.inventory.events.EventConsumer.jvmInstanceOf;
import static com.redhat.runtimes.inventory.events.Utils.readBytesFromResources;
import static com.redhat.runtimes.inventory.events.Utils.readFromResources;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
public class EventConsumerTest {

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
  public void testSimpleUnzip() throws IOException {
    var buffy = readBytesFromResources("1J6DOEu9ni-000029.gz");
    var json = EventConsumer.unzipJson(buffy);
    TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};
    var mapper = new ObjectMapper();
    var o = mapper.readValue(json, typeRef);
    var idHash =
        "1fe34df6b75eaf557e59f58b88f584f398ed4b73e41730e3e88c34d6052ae231c677bdc1a1c5ce22202c485ce4f101f66fc283c5a7f81f51c59b09806b481f22";
    assertEquals(idHash, o.get("idHash"));
  }

  @Test
  public void test_jvmInstance_MWTELE_66() throws IOException {
    var dummy = new ArchiveAnnouncement();
    dummy.setTimestamp(LocalDateTime.MAX);

    var buffy = readBytesFromResources("jdk8_MWTELE-66.gz");
    var json = EventConsumer.unzipJson(buffy);

    var msg = jvmInstanceOf(dummy, json);
    assertTrue(msg instanceof JvmInstance);
    var inst = (JvmInstance) msg;

    var hostname = "fedora";
    assertEquals(hostname, inst.getHostname());

    json = readFromResources("test17.json");
    msg = jvmInstanceOf(dummy, json);
    assertTrue(msg instanceof JvmInstance);
    inst = (JvmInstance) msg;

    hostname = "uriel.local";
    assertEquals(hostname, inst.getHostname());
  }

  @Test
  @Disabled
  public void test_jvmInstance_MWTELE_67() throws IOException {
    var dummy = new ArchiveAnnouncement();
    dummy.setTimestamp(LocalDateTime.MIN);

    var buffy = readBytesFromResources("update1.json.gz");
    var json = EventConsumer.unzipJson(buffy);
    var msg = jvmInstanceOf(dummy, json);
    assertTrue(msg instanceof JvmInstance);
    var inst = (JvmInstance) msg;

    var hostname = "fedora";
    assertEquals(hostname, inst.getHostname());
  }

  @Test
  // @Disabled
  public void test_EapInstance_example1() throws IOException {
    var dummy = new ArchiveAnnouncement();
    dummy.setTimestamp(LocalDateTime.MAX);

    var json = readFromResources("eap_example1.json");
    var msg = eapInstanceOf(dummy, json);
    assertTrue(msg instanceof EapInstance);
    var inst = (EapInstance) msg;

    var hostname = "freya";
    assertEquals(hostname, inst.getHostname());
    assertEquals(false, inst.getEapXp());
    assertEquals(
        "JBoss EAP 7.4.11.GA (WildFly Core 15.0.26.Final-redhat-00001)", inst.getEapVersion());
    assertEquals("7.4.11.GA", inst.getConfiguration().getProductVersion());
    // assertEquals(4708, inst.getModules().size());
    assertEquals(2, inst.getDeployments().size());

    assertEquals(2, inst.getConfiguration().getDeployments().size());
    assertEquals(39, inst.getConfiguration().getExtensions().size());
    assertNotNull(inst.getConfiguration().getCoreServices());
    assertNotNull(inst.getConfiguration().getInterfaces());
    assertNotNull(inst.getConfiguration().getPaths());
    assertNotNull(inst.getConfiguration().getSocketBindingGroups());
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
    dummy.setTimestamp(LocalDateTime.now());

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
