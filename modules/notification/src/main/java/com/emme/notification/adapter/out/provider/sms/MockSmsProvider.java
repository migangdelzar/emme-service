package com.emme.notification.adapter.out.provider.sms;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Always-available mock SMS provider. Logs messages and returns a mock ID. Used when no real
 * provider is configured or in dev/test environments.
 *
 * <p>Activated when app.notification.sms.provider is "mock" or not set.
 */
@Component
@ConditionalOnProperty(
    name = "app.notification.sms.provider",
    havingValue = "mock",
    matchIfMissing = true)
@Slf4j
public class MockSmsProvider implements com.emme.notification.application.port.out.SmsSender {

  @Override
  public String name() {
    return "mock";
  }

  @Override
  public String send(String to, String message) {
    String messageId = "mock-sms-" + UUID.randomUUID();
    log.info("MOCK SMS — to: {}, message: {}, id: {}", to, message, messageId);
    return messageId;
  }

  @Override
  public boolean isMock() {
    return true;
  }
}
