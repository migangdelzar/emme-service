package com.emme.tenancy.api.result;

import com.emme.tenancy.api.type.TenantStatus;
import java.time.Instant;
import java.util.UUID;

/** Public tenant read model returned by Tenancy use cases. */
public record TenantDetails(
    UUID id,
    String slug,
    String name,
    String schemaName,
    TenantStatus status,
    String databaseMode,
    String identityRealm,
    Instant createdAt) {
  public TenantDetails(
      UUID id,
      String slug,
      String name,
      String schemaName,
      TenantStatus status,
      String databaseMode,
      String identityRealm) {
    this(id, slug, name, schemaName, status, databaseMode, identityRealm, null);
  }
}
