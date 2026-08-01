package com.emme.assistant.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class WhatsAppPropertiesTest {

  @Test
  void appliesSafeDefaultsForOptionalWhatsAppSettings() {
    WhatsAppProperties properties = new WhatsAppProperties(null, null, null, null, null, null);

    assertThat(properties.verifyToken()).isEqualTo("emme_verify_token");
    assertThat(properties.appSecret()).isEmpty();
    assertThat(properties.defaultTenantId())
        .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    assertThat(properties.apiBaseUrl()).isEqualTo("https://graph.facebook.com/v21.0");
  }

  @Test
  void preservesConfiguredWhatsAppSettings() {
    UUID tenantId = UUID.randomUUID();
    WhatsAppProperties properties =
        new WhatsAppProperties(
            "verify", "secret", tenantId.toString(), "access", "phone", "https://graph.test");

    assertThat(properties.verifyToken()).isEqualTo("verify");
    assertThat(properties.appSecret()).isEqualTo("secret");
    assertThat(properties.defaultTenantId()).isEqualTo(tenantId);
    assertThat(properties.accessToken()).isEqualTo("access");
    assertThat(properties.phoneNumberId()).isEqualTo("phone");
    assertThat(properties.apiBaseUrl()).isEqualTo("https://graph.test");
  }
}
