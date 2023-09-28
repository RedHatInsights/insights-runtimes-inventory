/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.auth.principal;

import static com.redhat.runtimes.inventory.models.Constants.X_RH_IDENTITY_HEADER;

import com.redhat.runtimes.inventory.auth.principal.rhid.RhIdPrincipal;
import com.redhat.runtimes.inventory.auth.principal.rhid.RhIdentity;

// Derived from notifications-backend
public class ConsolePrincipalFactory {

  private static final String MISSING_ORG_ID_MSG =
      "The org_id field is missing or blank in the " + X_RH_IDENTITY_HEADER + " header";

  public static ConsolePrincipal<?> fromIdentity(ConsoleIdentity identity)
      throws IllegalIdentityHeaderException {
    if (identity instanceof RhIdentity) {
      RhIdPrincipal principal = new RhIdPrincipal((RhIdentity) identity);
      if (principal.getOrgId() == null || principal.getOrgId().isBlank()) {
        throw new IllegalIdentityHeaderException(MISSING_ORG_ID_MSG);
      }
      return principal;
    }

    throw new IllegalArgumentException(
        String.format(
            "Unprocessed identity found. type: %s and name: %s",
            identity.type, identity.getName()));
  }
}
