package com.emme.tenancy.api;

import java.util.UUID;

public record TenantInfo(
    UUID id,
    String slug,
    String name,
    String schemaName,
    String status,
    String databaseMode,
    String identityRealm) {}
