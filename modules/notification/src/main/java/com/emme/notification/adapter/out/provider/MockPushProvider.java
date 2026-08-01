package com.emme.notification.adapter.out.provider;

import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Always-available mock provider. Logs push payloads instead of sending them to a real gateway.
 * Active when no real provider is configured (default fallback).
 */
@Component
@ConditionalOnProperty(
    name = "app.notification.push.provider",
    havingValue = "mock",
    matchIfMissing = true)
public class MockPushProvider implements com.emme.notification.application.port.out.PushSender {

  private static final Logger log = LoggerFactory.getLogger(MockPushProvider.class);

  @Override
  public String name() {
    return "mock";
  }

  @Override
  public String send(String deviceToken, String title, String body, Map<String, String> data) {
    String messageId = "mock-push-" + UUID.randomUUID();
    log.info(
        "Mock push sent — id={} token={} title='{}' body='{}' data={}",
        messageId,
        deviceToken,
        title,
        body,
        data);
    return messageId;
  }

  @Override
  public boolean isMock() {
    return true;
  }
}
