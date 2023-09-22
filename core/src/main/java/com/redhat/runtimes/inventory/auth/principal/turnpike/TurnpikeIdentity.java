/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.auth.principal.turnpike;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.runtimes.inventory.auth.principal.ConsoleIdentity;

// Information about what's retrieved (depending the auth method) can be found on:
// https://internal.console.redhat.com/api/turnpike/identity/
public abstract class TurnpikeIdentity extends ConsoleIdentity {
  // In the future, Associates might be authenticated through other means

  @JsonProperty("auth_type")
  public String authType;
}
