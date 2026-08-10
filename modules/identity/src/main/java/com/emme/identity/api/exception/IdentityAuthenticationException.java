package com.emme.identity.api.exception;

/** Raised when the configured Identity provider rejects or cannot complete authentication. */
public final class IdentityAuthenticationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public IdentityAuthenticationException(String message) {
    super(message);
  }

  public IdentityAuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}
