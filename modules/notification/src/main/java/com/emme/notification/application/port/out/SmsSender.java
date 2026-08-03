package com.emme.notification.application.port.out;

/**
 * Abstraction for SMS notification providers (Twilio, MessageBird, Vonage, Mock). Implementations
 * handle SMS sending lifecycle.
 *
 * <p>Use {@code app.notification.sms.provider} to select: mock (default), twilio, messagebird,
 * vonage
 */
public interface SmsSender {

  /** Provider identifier (e.g. "twilio", "messagebird", "mock") */
  String name();

  /**
   * Send an SMS message and return the provider-specific message ID.
   *
   * <p>Implementations must throw a provider-specific runtime exception when the provider rejects
   * the message or cannot be reached. Returning an error string is forbidden because callers would
   * otherwise be unable to distinguish a delivered message ID from a failed operation.
   */
  String send(String to, String message);

  /** Whether this provider is a mock implementation */
  default boolean isMock() {
    return false;
  }
}
