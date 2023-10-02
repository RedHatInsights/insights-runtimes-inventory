/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.web;

import static com.redhat.runtimes.inventory.models.Constants.X_RH_IDENTITY_HEADER;
import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.emptyString;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Header;
import io.vertx.core.json.JsonObject;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/** Test ConsoleIdentityProvider. Brought over from notifications-backend and extended */
@QuarkusTest
public class ConsoleIdentityProviderIT {

  @Test
  void testNullOrgId() {
    Header identityHeader = buildIdentityHeader(null);
    given()
        .header(identityHeader)
        .when()
        .get("/api/runtimes-inventory-service/v1/instances/")
        .then()
        .statusCode(401)
        .body(
            emptyString()); // We must NOT leak security impl details such as a missing field in the
    // x-rh-identity header.
  }

  @Test
  void testEmptyOrgId() {
    Header identityHeader = buildIdentityHeader("");
    given()
        .header(identityHeader)
        .when()
        .get("/api/runtimes-inventory-service/v1/instances/")
        .then()
        .statusCode(401)
        .body(
            emptyString()); // We must NOT leak security impl details such as a missing field in the
    // x-rh-identity header.
  }

  @Test
  void testBlankOrgId() {
    Header identityHeader = buildIdentityHeader("   ");
    given()
        .header(identityHeader)
        .when()
        .get("/api/runtimes-inventory-service/v1/instances/")
        .then()
        .statusCode(401)
        .body(
            emptyString()); // We must NOT leak security impl details such as a missing field in the
    // x-rh-identity header.
  }

  private static Header buildIdentityHeader(String orgId) {
    String identityHeaderValue = encodeRHIdentityInfo("account-id", orgId, "johndoe");
    return createRHIdentityHeader(identityHeaderValue);
  }

  private static String encodeRHIdentityInfo(String accountId, String orgId, String username) {
    JsonObject identity = new JsonObject();
    JsonObject user = new JsonObject();
    user.put("username", username);
    identity.put("account_number", accountId);
    identity.put("org_id", orgId);
    identity.put("user", user);
    identity.put("type", "User");
    JsonObject header = new JsonObject();
    header.put("identity", identity);

    return new String(Base64.getEncoder().encode(header.encode().getBytes(UTF_8)), UTF_8);
  }

  private static Header createRHIdentityHeader(String encodedIdentityHeader) {
    return new Header(X_RH_IDENTITY_HEADER, encodedIdentityHeader);
  }
}
