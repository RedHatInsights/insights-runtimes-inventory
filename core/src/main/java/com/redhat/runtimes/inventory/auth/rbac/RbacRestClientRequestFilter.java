/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.auth.rbac;

import static java.nio.charset.StandardCharsets.UTF_8;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;

// Derived from notifications-backend

/**
 * Filter to optionally add data on outgoing requests to the RBAC service. This is meant for local
 * development and not production.
 */
public class RbacRestClientRequestFilter implements ClientRequestFilter {

  private String authInfo;

  public RbacRestClientRequestFilter() {
    String tmp = System.getProperty("develop.exceptional.user.auth.info");
    if (tmp != null && !tmp.isEmpty()) {
      authInfo = new String(Base64.getEncoder().encode(tmp.getBytes(UTF_8)), UTF_8);
    }
  }

  @Override
  public void filter(ClientRequestContext requestContext) throws IOException {

    if (authInfo != null) {
      URI uri = requestContext.getUri();
      if (uri.toString().startsWith("https://ci.cloud.redhat.com")) {
        requestContext.getHeaders().putSingle("Authorization", "Basic " + authInfo);
      }
    }
  }
}
