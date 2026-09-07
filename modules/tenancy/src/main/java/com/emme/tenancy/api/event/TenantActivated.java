package com.emme.tenancy.api.event;

import java.util.UUID;

public record TenantActivated(
    UUID eventId, UUID tenantId, String slug, String schemaName, String keycloakRealm) {

  public TenantActivated {
    if (eventId == null) eventId = UUID.randomUUID();
    if (schemaName == null || !schemaName.matches("[a-z0-9_]+")) {
      throw new IllegalArgumentException("Invalid tenant schema name");
    }
  }
}
