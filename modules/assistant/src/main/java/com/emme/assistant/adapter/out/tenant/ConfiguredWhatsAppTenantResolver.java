package com.emme.assistant.adapter.out.tenant;

import com.emme.assistant.application.port.out.WhatsAppTenantResolver;
import com.emme.assistant.configuration.WhatsAppProperties;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Resolves the explicitly configured WhatsApp account without accepting arbitrary tenant input. */
@Component
public class ConfiguredWhatsAppTenantResolver implements WhatsAppTenantResolver {
  private final WhatsAppProperties properties;

  public ConfiguredWhatsAppTenantResolver(WhatsAppProperties properties) {
    this.properties = properties;
  }

  @Override
  public UUID resolve(String providerAccountId) {
    if (providerAccountId == null
        || providerAccountId.isBlank()
        || properties.phoneNumberId().isBlank()
        || !properties.phoneNumberId().equals(providerAccountId)) {
      throw new SecurityException("Unknown WhatsApp provider account");
    }
    return properties.defaultTenantId();
  }
}
