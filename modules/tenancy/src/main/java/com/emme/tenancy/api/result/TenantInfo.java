package com.emme.tenancy.api.result;

import java.util.UUID;

/** Public tenant read model returned by Tenancy use cases. */
public record TenantInfo(
    UUID id,
    String slug,
    String name,
    String schemaName,
    String status,
    String databaseMode,
    String identityRealm) {}
