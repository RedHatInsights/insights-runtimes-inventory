/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.web;

import static com.redhat.runtimes.inventory.models.Constants.X_RH_IDENTITY_HEADER;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Header;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.util.Base64;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
@Tag("integration-tests")
public class DisplayInventoryTest {

  @Inject EntityManager entityManager;

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

  @Test
  public void testInstanceEndpointWithoutValidHeader() {
    given().when().get("/api/runtimes-inventory-service/v1/instance").then().statusCode(500);
  }

  @Test
  public void testInstanceEndpointWithValidHeader() throws IOException {
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
            .get("/api/runtimes-inventory-service/v1/instance")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
    assertEquals("{\"response\": \"[not found]\"}", response);
  }
}
