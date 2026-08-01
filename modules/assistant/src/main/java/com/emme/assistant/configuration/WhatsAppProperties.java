package com.emme.assistant.configuration;

import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed WhatsApp webhook and Cloud API settings bound to {@code app.whatsapp.*}. */
@ConfigurationProperties(prefix = "app.whatsapp")
public record WhatsAppProperties(
    String verifyToken,
    String appSecret,
    String tenantId,
    String accessToken,
    String phoneNumberId,
    String apiBaseUrl) {

  public WhatsAppProperties {
    verifyToken = verifyToken == null ? "" : verifyToken;
    appSecret = appSecret == null ? "" : appSecret;
    tenantId = tenantId == null ? "" : tenantId;
    accessToken = accessToken == null ? "" : accessToken;
    phoneNumberId = phoneNumberId == null ? "" : phoneNumberId;
    apiBaseUrl = apiBaseUrl == null ? "https://graph.facebook.com/v21.0" : apiBaseUrl;
  }

  /** Returns the configured default tenant, failing fast when the value is malformed. */
  public UUID defaultTenantId() {
    if (tenantId.isBlank()) {
      throw new IllegalStateException("app.whatsapp.tenant-id must be configured");
    }
    try {
      return UUID.fromString(tenantId);
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("app.whatsapp.tenant-id must be a UUID", exception);
    }
  }
}
