package com.emme.notification.adapter.out.provider.email;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Always-available mock provider. Logs email content to SLF4J without sending anything. Used in
 * dev/test or when no real email provider is configured.
 */
@Component
@ConditionalOnProperty(
    name = "app.notification.email.provider",
    havingValue = "mock",
    matchIfMissing = true)
@Slf4j
public class MockEmailProvider implements com.emme.notification.application.port.out.EmailSender {

  @Override
  public String name() {
    return "mock";
  }

  @Override
  public String send(String to, String subject, String body, String html) {
    String messageId = "mock-email-" + UUID.randomUUID();
    log.info(
        "[MockEmail] To: {} | Subject: {} | ID: {} | Body: {} | HTML: {}",
        to,
        subject,
        messageId,
        body,
        html != null);
    return messageId;
  }

  @Override
  public boolean isMock() {
    return true;
  }
}
