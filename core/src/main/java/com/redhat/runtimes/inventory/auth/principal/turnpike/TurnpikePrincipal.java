/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.auth.principal.turnpike;

import com.redhat.runtimes.inventory.auth.principal.ConsolePrincipal;

public class TurnpikePrincipal extends ConsolePrincipal<TurnpikeIdentity> {
  public TurnpikePrincipal(TurnpikeIdentity identity) {
    super(identity);
  }
}
