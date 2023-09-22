/* Copyright (C) Red Hat 2021-2023 */
package com.redhat.runtimes.inventory.auth.principal.turnpike;

/** author hrupp */
public class TurnpikeX509Identity extends TurnpikeIdentity {
  public X509 x509;

  @Override
  public String getName() {
    return x509.subject_dn;
  }

  public class X509 {
    public String subject_dn;
    public String issuer_dn;
  }
}
