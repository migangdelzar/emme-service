package com.emme.notification.provider;

/**
 * Thrown when a push notification provider operation fails (API errors, authentication failures,
 * invalid tokens, etc.).
 */
public class PushProviderException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public PushProviderException(String message) {
    super(message);
  }

  public PushProviderException(String message, Throwable cause) {
    super(message, cause);
  }
}
