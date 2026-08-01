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

  /** Send an SMS message. Returns provider-specific message ID. */
  String send(String to, String message);

  /** Whether this provider is a mock implementation */
  default boolean isMock() {
    return false;
  }
}
