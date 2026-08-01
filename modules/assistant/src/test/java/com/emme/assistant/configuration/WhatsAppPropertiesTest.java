package com.emme.assistant.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class WhatsAppPropertiesTest {

  @Test
  void appliesSafeDefaultsForOptionalWhatsAppSettings() {
    WhatsAppProperties properties = new WhatsAppProperties(null, null, null, null, null, null);

    assertThat(properties.verifyToken()).isEmpty();
    assertThat(properties.appSecret()).isEmpty();
    assertThatThrownBy(properties::defaultTenantId).isInstanceOf(IllegalStateException.class);
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
