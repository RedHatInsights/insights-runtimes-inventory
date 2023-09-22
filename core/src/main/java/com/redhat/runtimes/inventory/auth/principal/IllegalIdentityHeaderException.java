/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.auth.principal;

public class IllegalIdentityHeaderException extends Exception {

  public IllegalIdentityHeaderException(String message) {
    super(message);
  }
}
