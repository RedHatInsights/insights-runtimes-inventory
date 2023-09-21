/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.web;

import static com.redhat.runtimes.inventory.models.Constants.X_RH_IDENTITY_HEADER;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.runtimes.inventory.events.ArchiveAnnouncement;
import com.redhat.runtimes.inventory.events.EventConsumer;
import com.redhat.runtimes.inventory.events.TestUtils;
import com.redhat.runtimes.inventory.events.Utils;
import com.redhat.runtimes.inventory.models.EapInstance;
import com.redhat.runtimes.inventory.models.InsightsMessage;
import com.redhat.runtimes.inventory.models.JvmInstance;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Header;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
@Tag("integration-tests")
public class DisplayInventoryTest {

  @Inject EntityManager entityManager;

  @AfterEach
  @Transactional
  void tearDown() {
    entityManager.createNativeQuery("DELETE FROM eap_instance_jar_hash").executeUpdate();
    TestUtils.clearTables(entityManager);
  }

  private static Header createRHIdentityHeader(String encodedIdentityHeader) {
    return new Header(X_RH_IDENTITY_HEADER, encodedIdentityHeader);
  }

  private static String encode(String value) {
    return new String(Base64.getEncoder().encode(value.getBytes()));
  }

  private static String encodeRHIdentityInfo(String accountNumber, String orgId, String username) {
    ObjectMapper mapper = new ObjectMapper();

    ObjectNode user = mapper.createObjectNode();
    user.put("username", username);

    ObjectNode identity = mapper.createObjectNode();
    identity.put("account_number", accountNumber);
    identity.put("org_id", orgId);
    identity.set("user", user);
    identity.put("type", "User");

    ObjectNode head = mapper.createObjectNode();
    head.set("identity", identity);

    return encode(head.toString());
  }

  private ArchiveAnnouncement setupArchiveAnnouncement() {
    ArchiveAnnouncement announcement = new ArchiveAnnouncement();
    announcement.setAccountId("accountId");
    announcement.setOrgId("orgId");
    announcement.setTimestamp(Instant.now());
    return announcement;
  }

  private JvmInstance getJvmInstanceFromZipJsonFile(String filename) throws IOException {
    byte[] buffy = TestUtils.readBytesFromResources(filename);
    String json = EventConsumer.unzipJson(buffy);
    InsightsMessage message = Utils.jvmInstanceOf(setupArchiveAnnouncement(), json);
    assertTrue(message instanceof JvmInstance);
    JvmInstance instance = (JvmInstance) message;
    return instance;
  }

  private JvmInstance getJvmInstanceFromJsonFile(String filename) throws IOException {
    String json = TestUtils.readFromResources(filename);
    InsightsMessage message = Utils.jvmInstanceOf(setupArchiveAnnouncement(), json);
    assertTrue(message instanceof JvmInstance);
    JvmInstance instance = (JvmInstance) message;
    return instance;
  }

  private EapInstance getEapInstanceFromJsonFile(String filename) throws IOException {
    String json = TestUtils.readFromResources(filename);
    InsightsMessage message = Utils.eapInstanceOf(setupArchiveAnnouncement(), json);
    assertTrue(message instanceof EapInstance);
    EapInstance instance = (EapInstance) message;
    return instance;
  }

  @Transactional
  void persistInstanceToDatabase(Object instance) {
    entityManager.persist(instance);
  }

  @Test
  void testJvmInstanceEndpointWithoutValidHeader() {
    given().when().get("/api/runtimes-inventory-service/v1/instance").then().statusCode(500);
  }

  @Test
  void testJvmInstanceEndpointsWithInvalidGroupId() {
    Header identityHeader = createRHIdentityHeader(encode("not-a-real-identity"));
    String[] endpoints = {"instance-ids", "instance", "instances"};
    for (String endpoint : endpoints) {
      String response =
          given()
              .header(identityHeader)
              .when()
              .queryParam("hostname", "fedora")
              .get("/api/runtimes-inventory-service/v1/" + endpoint)
              .then()
              .statusCode(200)
              .extract()
              .body()
              .asString();
      assertEquals("{\"response\": \"[error]\"}", response);
    }
  }

  @Test
  void testJvmInstanceIdsWithNoJvmInstances() throws IOException {
    String accountNumber = "accountId";
    String orgId = "orgId";
    String username = "user";
    String identityHeaderValue = encodeRHIdentityInfo(accountNumber, orgId, username);
    Header identityHeader = createRHIdentityHeader(identityHeaderValue);
    String response =
        given()
            .header(identityHeader)
            .when()
            .queryParam("hostname", "fedora")
            .get("/api/runtimes-inventory-service/v1/instance-ids/")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
    assertEquals("{\"response\": \"[]\"}", response);
  }

  @Test
  void testJvmInstanceIdsWithSingleJvmInstance() throws IOException {
    JvmInstance instance = getJvmInstanceFromZipJsonFile("jdk8_MWTELE-66.gz");
    persistInstanceToDatabase(instance);
    String accountNumber = "accountId";
    String orgId = "orgId";
    String username = "user";
    String identityHeaderValue = encodeRHIdentityInfo(accountNumber, orgId, username);
    Header identityHeader = createRHIdentityHeader(identityHeaderValue);
    String response =
        given()
            .header(identityHeader)
            .when()
            .queryParam("hostname", instance.getHostname())
            .get("/api/runtimes-inventory-service/v1/instance-ids/")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(response).get("response");
    assertEquals(true, jsonNode.isArray());
    assertEquals(1, jsonNode.size());
  }

  @Test
  void testJvmInstanceIdsWithMultipleJvmInstances() throws IOException {
    persistInstanceToDatabase(getJvmInstanceFromZipJsonFile("jdk8_MWTELE-66.gz"));
    JvmInstance modifiedInstance = getJvmInstanceFromJsonFile("test17.json");
    // set the hostname to match the previous instance
    modifiedInstance.setHostname("fedora");
    persistInstanceToDatabase(modifiedInstance);
    assertEquals(2L, TestUtils.entity_count(entityManager, "JvmInstance"));

    String accountNumber = "accountId";
    String orgId = "orgId";
    String username = "user";
    String identityHeaderValue = encodeRHIdentityInfo(accountNumber, orgId, username);
    Header identityHeader = createRHIdentityHeader(identityHeaderValue);
    String response =
        given()
            .header(identityHeader)
            .when()
            .queryParam("hostname", modifiedInstance.getHostname())
            .get("/api/runtimes-inventory-service/v1/instance-ids/")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(response).get("response");
    assertEquals(true, jsonNode.isArray());
    assertEquals(2, jsonNode.size());
  }

  @Test
  void testJvmInstanceWithValidId() throws IOException {
    JvmInstance instance = getJvmInstanceFromZipJsonFile("jdk8_MWTELE-66.gz");
    persistInstanceToDatabase(instance);
    String accountNumber = "accountId";
    String orgId = "orgId";
    String username = "user";
    String identityHeaderValue = encodeRHIdentityInfo(accountNumber, orgId, username);
    Header identityHeader = createRHIdentityHeader(identityHeaderValue);
    String response =
        given()
            .header(identityHeader)
            .when()
            .queryParam("hostname", instance.getHostname())
            .get("/api/runtimes-inventory-service/v1/instance-ids/")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
    List<String> ids = mapResponseIdsToList(response);
    assertEquals(1, ids.size());
    String secondResponse =
        given()
            .header(identityHeader)
            .when()
            .queryParam("jvmInstanceId", ids.get(0))
            .get("/api/runtimes-inventory-service/v1/instance/")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode responseNode = mapper.readTree(secondResponse).get("response");
    assertEquals(instance.getId(), UUID.fromString(responseNode.get("id").asText()));
    assertEquals(instance.getLaunchTime(), responseNode.get("launchTime").asLong());
    assertEquals(instance.getVendor(), responseNode.get("vendor").asText());
    assertEquals(instance.getVersionString(), responseNode.get("versionString").asText());
    assertEquals(instance.getHeapMin(), responseNode.get("heapMin").asInt());
  }

  @Test
  void testJvmInstanceWithInvalidId() throws IOException {
    JvmInstance instance = getJvmInstanceFromZipJsonFile("jdk8_MWTELE-66.gz");
    persistInstanceToDatabase(instance);
    String accountNumber = "accountId";
    String orgId = "orgId";
    String username = "user";
    String identityHeaderValue = encodeRHIdentityInfo(accountNumber, orgId, username);
    Header identityHeader = createRHIdentityHeader(identityHeaderValue);
    given()
        .header(identityHeader)
        .when()
        .queryParam("hostname", instance.getHostname())
        .get("/api/runtimes-inventory-service/v1/instance-ids/")
        .then()
        .statusCode(200);
    String response =
        given()
            .header(identityHeader)
            .when()
            .queryParam("jvmInstanceId", UUID.randomUUID().toString())
            .get("/api/runtimes-inventory-service/v1/instance/")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
    assertEquals("{\"response\": \"[]\"}", response);
  }

  @Test
  void testJvmInstancesWithNoInstances() {
    String accountNumber = "accountId";
    String orgId = "orgId";
    String username = "user";
    String identityHeaderValue = encodeRHIdentityInfo(accountNumber, orgId, username);
    Header identityHeader = createRHIdentityHeader(identityHeaderValue);
    String response =
        given()
            .header(identityHeader)
            .when()
            .queryParam("hostname", "fedora")
            .get("/api/runtimes-inventory-service/v1/instances/")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
    assertEquals("{\"response\": \"[]\"}", response);
  }

  @Test
  void testJvmInstancesWithMultipleJvmInstances() throws IOException {
    persistInstanceToDatabase(getJvmInstanceFromZipJsonFile("jdk8_MWTELE-66.gz"));
    JvmInstance modifiedInstance = getJvmInstanceFromJsonFile("test17.json");
    // set the hostname to match the previous instance
    modifiedInstance.setHostname("fedora");
    persistInstanceToDatabase(modifiedInstance);
    assertEquals(2L, TestUtils.entity_count(entityManager, "JvmInstance"));
    String accountNumber = "accountId";
    String orgId = "orgId";
    String username = "user";
    String identityHeaderValue = encodeRHIdentityInfo(accountNumber, orgId, username);
    Header identityHeader = createRHIdentityHeader(identityHeaderValue);
    String response =
        given()
            .header(identityHeader)
            .when()
            .queryParam("hostname", modifiedInstance.getHostname())
            .get("/api/runtimes-inventory-service/v1/instances/")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(response).get("response");
    assertEquals(true, jsonNode.isArray());
    assertEquals(2, jsonNode.size());
  }

  @Test
  void testJarHashesWithNoRecords() throws IOException {
    String response =
        given()
            .when()
            .queryParam("jvmInstanceId", UUID.randomUUID().toString())
            .get("/api/runtimes-inventory-service/v1/jarhashes/")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
    assertEquals("{\"response\": \"[]\"}", response);
  }

  @Test
  void testJarHashesWithSingleRecord() throws IOException {
    // this jvm instance has 1 jar hash associated with it
    JvmInstance instance = getJvmInstanceFromZipJsonFile("jdk8_MWTELE-66.gz");
    persistInstanceToDatabase(instance);
    String accountNumber = "accountId";
    String orgId = "orgId";
    String username = "user";
    String identityHeaderValue = encodeRHIdentityInfo(accountNumber, orgId, username);
    Header identityHeader = createRHIdentityHeader(identityHeaderValue);
    String jvmInstanceIdsResponse =
        given()
            .header(identityHeader)
            .when()
            .queryParam("hostname", instance.getHostname())
            .get("/api/runtimes-inventory-service/v1/instance-ids/")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
    List<String> ids = mapResponseIdsToList(jvmInstanceIdsResponse);
    assertEquals(1, ids.size());
    String response =
        given()
            .when()
            .queryParam("jvmInstanceId", UUID.fromString(ids.get(0)))
            .get("/api/runtimes-inventory-service/v1/jarhashes/")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(response).get("response");
    assertEquals(true, jsonNode.isArray());
    assertEquals(1, jsonNode.size());
  }

  @Test
  void testJarHashesWithMultipleRecords() throws IOException {
    // this jvm instance has 11 jar hashes associated with it
    JvmInstance instance = getJvmInstanceFromJsonFile("test17.json");
    persistInstanceToDatabase(getJvmInstanceFromJsonFile("test17.json"));
    String accountNumber = "accountId";
    String orgId = "orgId";
    String username = "user";
    String identityHeaderValue = encodeRHIdentityInfo(accountNumber, orgId, username);
    Header identityHeader = createRHIdentityHeader(identityHeaderValue);
    String jvmInstanceIdsResponse =
        given()
            .header(identityHeader)
            .when()
            .queryParam("hostname", instance.getHostname())
            .get("/api/runtimes-inventory-service/v1/instance-ids/")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
    List<String> ids = mapResponseIdsToList(jvmInstanceIdsResponse);
    assertEquals(1, ids.size());
    String response =
        given()
            .when()
            .queryParam("jvmInstanceId", UUID.fromString(ids.get(0)))
            .get("/api/runtimes-inventory-service/v1/jarhashes/")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(response).get("response");
    assertEquals(true, jsonNode.isArray());
    assertEquals(11, jsonNode.size());
  }

  @Test
  void testEapInstanceEndpointsWithInvalidGroupId() {
    Header identityHeader = createRHIdentityHeader(encode("not-a-real-identity"));
    String[] endpoints = {"eap-instance-ids", "eap-instance", "eap-instances"};
    for (String endpoint : endpoints) {
      String response =
          given()
              .header(identityHeader)
              .when()
              .queryParam("hostname", "fedora")
              .get("/api/runtimes-inventory-service/v1/" + endpoint)
              .then()
              .statusCode(200)
              .extract()
              .body()
              .asString();
      assertEquals("{\"response\": \"[error]\"}", response);
    }
  }

  @Test
  void testEapInstanceIdsWithNoEapInstances() throws IOException {
    String accountNumber = "accountId";
    String orgId = "orgId";
    String username = "user";
    String identityHeaderValue = encodeRHIdentityInfo(accountNumber, orgId, username);
    Header identityHeader = createRHIdentityHeader(identityHeaderValue);
    String response =
        given()
            .header(identityHeader)
            .when()
            .queryParam("hostname", "freya")
            .get("/api/runtimes-inventory-service/v1/eap-instance-ids/")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
    assertEquals("{\"response\": \"[]\"}", response);
  }

  @Test
  void testEapInstanceIdsWithSingleEapInstance() throws IOException {
    EapInstance instance = getEapInstanceFromJsonFile("eap_example1.json");
    persistInstanceToDatabase(instance);
    String accountNumber = "accountId";
    String orgId = "orgId";
    String username = "user";
    String identityHeaderValue = encodeRHIdentityInfo(accountNumber, orgId, username);
    Header identityHeader = createRHIdentityHeader(identityHeaderValue);
    String response =
        given()
            .header(identityHeader)
            .when()
            .queryParam("hostname", instance.getHostname())
            .get("/api/runtimes-inventory-service/v1/eap-instance-ids/")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(response).get("response");
    assertEquals(true, jsonNode.isArray());
    assertEquals(1, jsonNode.size());
  }

  @Test
  void testEapInstanceWithValidId() throws IOException {
    EapInstance instance = getEapInstanceFromJsonFile("eap_example1.json");
    persistInstanceToDatabase(instance);
    String accountNumber = "accountId";
    String orgId = "orgId";
    String username = "user";
    String identityHeaderValue = encodeRHIdentityInfo(accountNumber, orgId, username);
    Header identityHeader = createRHIdentityHeader(identityHeaderValue);
    String response =
        given()
            .header(identityHeader)
            .when()
            .queryParam("hostname", instance.getHostname())
            .get("/api/runtimes-inventory-service/v1/eap-instance-ids/")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
    List<String> ids = mapResponseIdsToList(response);
    assertEquals(1, ids.size());
    String secondResponse =
        given()
            .header(identityHeader)
            .when()
            .queryParam("eapInstanceId", ids.get(0))
            .get("/api/runtimes-inventory-service/v1/eap-instance/")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode responseNode = mapper.readTree(secondResponse).get("response");
    assertEquals(instance.getId(), UUID.fromString(responseNode.get("id").asText()));
    assertEquals(instance.getEapVersion(), responseNode.get("eapVersion").asText());
    assertEquals(instance.getEapXp(), responseNode.get("eapXp").asBoolean());
  }

  @Test
  void testEapInstanceWithInvalidId() throws IOException {
    EapInstance instance = getEapInstanceFromJsonFile("eap_example1.json");
    persistInstanceToDatabase(instance);
    String accountNumber = "accountId";
    String orgId = "orgId";
    String username = "user";
    String identityHeaderValue = encodeRHIdentityInfo(accountNumber, orgId, username);
    Header identityHeader = createRHIdentityHeader(identityHeaderValue);
    given()
        .header(identityHeader)
        .when()
        .queryParam("hostname", instance.getHostname())
        .get("/api/runtimes-inventory-service/v1/eap-instance-ids/")
        .then()
        .statusCode(200);
    String response =
        given()
            .header(identityHeader)
            .when()
            .queryParam("eapInstanceId", UUID.randomUUID().toString())
            .get("/api/runtimes-inventory-service/v1/eap-instance/")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
    assertEquals("{\"response\": \"[]\"}", response);
  }

  @Test
  void testEapInstancesWithNoInstances() {
    String accountNumber = "accountId";
    String orgId = "orgId";
    String username = "user";
    String identityHeaderValue = encodeRHIdentityInfo(accountNumber, orgId, username);
    Header identityHeader = createRHIdentityHeader(identityHeaderValue);
    String response =
        given()
            .header(identityHeader)
            .when()
            .queryParam("hostname", "fedora")
            .get("/api/runtimes-inventory-service/v1/eap-instances/")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
    assertEquals("{\"response\": \"[]\"}", response);
  }

  @Test
  void testEapInstances() throws IOException {
    EapInstance instance = getEapInstanceFromJsonFile("eap_example1.json");
    persistInstanceToDatabase(instance);
    String accountNumber = "accountId";
    String orgId = "orgId";
    String username = "user";
    String identityHeaderValue = encodeRHIdentityInfo(accountNumber, orgId, username);
    Header identityHeader = createRHIdentityHeader(identityHeaderValue);
    String response =
        given()
            .header(identityHeader)
            .when()
            .queryParam("hostname", instance.getHostname())
            .get("/api/runtimes-inventory-service/v1/eap-instances/")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(response).get("response");
    assertEquals(true, jsonNode.isArray());
    assertEquals(1, jsonNode.size());
  }

  private List<String> mapResponseIdsToList(String response)
      throws JsonMappingException, JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(response).get("response");
    List<String> ids = new ArrayList<String>();
    if (jsonNode.isArray()) {
      for (JsonNode node : jsonNode) {
        ids.add(node.asText());
      }
    }
    return ids;
  }
}
