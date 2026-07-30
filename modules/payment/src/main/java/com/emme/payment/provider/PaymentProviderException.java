package com.emme.payment.provider;

/** Thrown when a payment provider operation fails (API errors, invalid state transitions, etc.). */
public class PaymentProviderException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public PaymentProviderException(String message) {
    super(message);
  }

  public PaymentProviderException(String message, Throwable cause) {
    super(message, cause);
  }
}
