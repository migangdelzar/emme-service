package com.emme.tenancy.api.event;

import java.util.UUID;

public record TenantRealmReady(UUID eventId, UUID tenantId, String slug, String keycloakRealm) {

  public TenantRealmReady {
    if (eventId == null) eventId = UUID.randomUUID();
  }
}
