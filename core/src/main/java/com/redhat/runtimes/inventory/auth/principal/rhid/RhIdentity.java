/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.auth.principal.rhid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.runtimes.inventory.auth.principal.ConsoleIdentity;

// Derived from notifications-backend
@JsonIgnoreProperties(ignoreUnknown = true)
public class RhIdentity extends ConsoleIdentity {

  @JsonProperty("account_number")
  private String accountNumber;

  @JsonProperty("org_id")
  private String orgId;

  private User user;

  public String getAccountNumber() {
    return accountNumber;
  }

  public User getUser() {
    return user;
  }

  public String getOrgId() {
    return orgId;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class User {
    @JsonProperty("username")
    private String username;

    public String getUsername() {
      return username;
    }
  }

  @Override
  public String getName() {
    return getUser().getUsername();
  }
}
