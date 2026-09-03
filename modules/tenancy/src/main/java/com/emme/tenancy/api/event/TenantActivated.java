package com.emme.tenancy.api.event;

import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("emme.tenancy.tenant-activated::#{#this.tenantId()}")
public record TenantActivated(
    UUID eventId, UUID tenantId, String slug, String schemaName, String keycloakRealm) {

  public TenantActivated {
    if (eventId == null) eventId = UUID.randomUUID();
    if (schemaName == null || !schemaName.matches("[a-z0-9_]+")) {
      throw new IllegalArgumentException("Invalid tenant schema name");
    }
  }
}
