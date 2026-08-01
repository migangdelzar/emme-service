package com.emme.identity.api.exception;

/** Raised when Identity cannot reach or decode a response from the provider. */
public final class IdentityProviderUnavailableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public IdentityProviderUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
