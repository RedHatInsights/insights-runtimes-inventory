/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.auth.principal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Derived from notifications-backend
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsoleIdentityWrapper {
  private ConsoleIdentity identity;

  public ConsoleIdentity getIdentity() {
    return identity;
  }
}
