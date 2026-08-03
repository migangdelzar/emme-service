package com.emme.notification.application.port.out;

/**
 * Abstraction for email notification providers (SMTP, SendGrid, AWS SES, Mock). Implementations
 * handle sending transactional and marketing emails.
 */
public interface EmailSender {

  /** Provider identifier (e.g. "smtp", "sendgrid", "ses", "mock") */
  String name();

  /**
   * Send an email. HTML content optional — falls back to plain text.
   *
   * @return provider-specific message ID
   */
  String send(String to, String subject, String body, String html);

  /** Whether this provider is a mock (no real emails sent) */
  default boolean isMock() {
    return false;
  }
}
