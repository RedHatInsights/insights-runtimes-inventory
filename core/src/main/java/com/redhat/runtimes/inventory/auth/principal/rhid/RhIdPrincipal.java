/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.auth.principal.rhid;

import com.redhat.runtimes.inventory.auth.principal.ConsolePrincipal;

public class RhIdPrincipal extends ConsolePrincipal<RhIdentity> {

  public RhIdPrincipal(RhIdentity identity) {
    super(identity);
  }

  public String getAccount() {
    return getIdentity().getAccountNumber();
  }

  public String getOrgId() {
    return getIdentity().getOrgId();
  }
}
