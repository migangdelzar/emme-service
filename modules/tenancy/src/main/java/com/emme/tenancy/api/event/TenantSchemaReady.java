package com.emme.tenancy.api.event;

import java.util.UUID;

/** Public fact emitted after a tenant schema has been provisioned. */
public record TenantSchemaReady(UUID eventId, UUID tenantId, String slug, String schemaName) {

  public TenantSchemaReady {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
  }
}
