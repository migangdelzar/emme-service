package com.emme.tenancy.api.event;

import java.util.UUID;

/** Public fact emitted after a tenant is persisted. */
public record TenantCreated(UUID tenantId, String slug, String name, String adminEmail) {}
