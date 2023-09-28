/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory;

import static com.redhat.runtimes.inventory.models.Constants.X_RH_IDENTITY_HEADER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.ClearType;

// Derived from notifications-backend
public class MockServerConfig {

  public enum RbacAccess {
    FULL_ACCESS(getFileAsString("rbac-examples/rbac_example_full_access.json")),
    INVENTORY_READ_ACCESS_ONLY(
        getFileAsString("rbac-examples/rbac_example_events_inventory_read_access_only.json")),
    HOSTS_ACCESS_ONLY(getFileAsString("rbac-examples/rbac_example_events_hosts_access_only.json")),
    READ_ACCESS(getFileAsString("rbac-examples/rbac_example_read_access.json")),
    NO_ACCESS(getFileAsString("rbac-examples/rbac_example_no_access.json"));

    private final String payload;

    RbacAccess(String payload) {
      this.payload = payload;
    }

    public String getPayload() {
      return payload;
    }
  }

  public static void addMockRbacAccess(
      ClientAndServer client, String xRhIdentity, RbacAccess access) {
    client
        .when(
            request()
                .withPath("/api/rbac/v1/access/")
                .withQueryStringParameter("application", "inventory")
                .withHeader(X_RH_IDENTITY_HEADER, xRhIdentity))
        .respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody(access.getPayload()));
  }

  public static void clearRbac(ClientAndServer client) {
    client.clear(request().withPath("/api/rbac/v1/access/"), ClearType.EXPECTATIONS);
  }

  private static String getFileAsString(String filename) {
    try (InputStream is = MockServerConfig.class.getClassLoader().getResourceAsStream(filename)) {
      return IOUtils.toString(is, UTF_8);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Failed to read RBAC example file: " + filename, e);
      return "";
    }
  }
}
