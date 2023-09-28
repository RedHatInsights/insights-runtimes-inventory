/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.auth;

import static com.redhat.runtimes.inventory.models.Constants.X_RH_IDENTITY_HEADER;

import io.quarkus.security.identity.request.BaseAuthenticationRequest;

// Derived from notifications-backend
public class ConsoleAuthenticationRequest extends BaseAuthenticationRequest {

  public ConsoleAuthenticationRequest(String xRhIdentity) {
    setAttribute(X_RH_IDENTITY_HEADER, xRhIdentity);
  }
}
