/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.auth.principal;

import java.security.Principal;

// Derived from notifications-backend
public abstract class ConsolePrincipal<T extends ConsoleIdentity> implements Principal {
  private final String name;
  private final T identity;
  private static final ConsolePrincipal<ConsoleIdentity> NO_IDENTITY_PRINCIPAL =
      new ConsolePrincipal<>() {};

  private ConsolePrincipal() {
    this.identity = null;
    this.name = "-noauth-";
  }

  public ConsolePrincipal(T identity) {
    this.identity = identity;
    this.name = identity.getName();
  }

  @Override
  public String getName() {
    return name;
  }

  public T getIdentity() {
    return this.identity;
  }

  public static ConsolePrincipal<ConsoleIdentity> noIdentity() {
    return NO_IDENTITY_PRINCIPAL;
  }
}
