package com.emme.identity.api.exception;

/** Raised when a customer provider token is validly decoded but not customer-scoped. */
public final class InvalidCustomerTokenException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  public InvalidCustomerTokenException() {
    super("Customer token is invalid");
  }
}
