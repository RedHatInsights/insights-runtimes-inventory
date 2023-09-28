/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.auth.rbac;

import io.quarkus.logging.Log;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.Response;
import java.io.IOException;

// Derived from notifications-backend

/**
 * Filter to look at the response (code) from the Rbac server. Log a warning if we have trouble
 * reaching the server.
 */
public class RbacClientResponseFilter implements ClientResponseFilter {

  @Override
  public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext)
      throws IOException {
    Response.StatusType statusInfo = responseContext.getStatusInfo();
    int status = statusInfo.getStatusCode();
    if (status == 0) {
      Log.infof(
          "Call to the Rbac server failed with code %d, %s", status, statusInfo.getReasonPhrase());
    } else if (status != 200) {
      Log.warnf(
          "Call to the Rbac server failed with code %d, %s", status, statusInfo.getReasonPhrase());
    }
  }
}
