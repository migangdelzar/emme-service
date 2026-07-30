package com.emme.notification.provider;

/** Runtime exception for email provider failures. */
public class EmailProviderException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public EmailProviderException(String message) {
    super(message);
  }

  public EmailProviderException(String message, Throwable cause) {
    super(message, cause);
  }
}
