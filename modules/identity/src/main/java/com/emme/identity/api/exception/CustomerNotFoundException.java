package com.emme.identity.api.exception;

import java.util.UUID;

/** Raised when a requested customer identity does not exist. */
public final class CustomerNotFoundException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  public CustomerNotFoundException(UUID customerId) {
    super("Customer not found: " + customerId);
  }
}
