package com.emme.notification.adapter.out.provider.sms;

/** Runtime failure raised when an SMS provider cannot accept a message. */
public final class SmsProviderException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public SmsProviderException(String message) {
    super(message);
  }

  public SmsProviderException(String message, Throwable cause) {
    super(message, cause);
  }
}
