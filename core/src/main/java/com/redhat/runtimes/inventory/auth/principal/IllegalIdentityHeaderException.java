/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.auth.principal;

// Derived from notifications-backend
public class IllegalIdentityHeaderException extends Exception {

  public IllegalIdentityHeaderException() {
    super();
  }

  public IllegalIdentityHeaderException(String message) {
    super(message);
  }

  public IllegalIdentityHeaderException(String message, Throwable cause) {
    super(message, cause);
  }

  public IllegalIdentityHeaderException(Throwable cause) {
    super(cause);
  }
}
