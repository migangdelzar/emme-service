package com.emme.tenancy.api.result;

import java.time.Instant;
import java.util.UUID;

/** Public tenant read model returned by Tenancy use cases. */
public record TenantInfo(
    UUID id,
    String slug,
    String name,
    String schemaName,
    String status,
    String databaseMode,
    String identityRealm,
    Instant createdAt) {
  public TenantInfo(
      UUID id,
      String slug,
      String name,
      String schemaName,
      String status,
      String databaseMode,
      String identityRealm) {
    this(id, slug, name, schemaName, status, databaseMode, identityRealm, null);
  }
}
