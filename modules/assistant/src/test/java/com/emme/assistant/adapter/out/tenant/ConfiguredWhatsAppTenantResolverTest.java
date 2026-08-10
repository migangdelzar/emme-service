package com.emme.assistant.adapter.out.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.assistant.configuration.WhatsAppProperties;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConfiguredWhatsAppTenantResolverTest {

  @Test
  void resolvesOnlyTheConfiguredProviderAccount() {
    UUID tenantId = UUID.randomUUID();
    ConfiguredWhatsAppTenantResolver resolver =
        new ConfiguredWhatsAppTenantResolver(
            new WhatsAppProperties(
                "verify",
                "secret",
                tenantId.toString(),
                "access",
                "phone-1",
                "https://graph.test"));

    assertThat(resolver.resolve("phone-1")).isEqualTo(tenantId);
    assertThatThrownBy(() -> resolver.resolve("unknown-phone"))
        .isInstanceOf(SecurityException.class);
  }
}
