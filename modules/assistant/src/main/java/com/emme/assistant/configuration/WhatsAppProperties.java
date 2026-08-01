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

  private static final String FALLBACK_TENANT_ID = "00000000-0000-0000-0000-000000000000";

  public WhatsAppProperties {
    verifyToken = verifyToken == null ? "emme_verify_token" : verifyToken;
    appSecret = appSecret == null ? "" : appSecret;
    tenantId = tenantId == null ? FALLBACK_TENANT_ID : tenantId;
    accessToken = accessToken == null ? "" : accessToken;
    phoneNumberId = phoneNumberId == null ? "" : phoneNumberId;
    apiBaseUrl = apiBaseUrl == null ? "https://graph.facebook.com/v21.0" : apiBaseUrl;
  }

  /** Returns the configured default tenant, failing fast when the value is malformed. */
  public UUID defaultTenantId() {
    return UUID.fromString(tenantId);
  }
}
