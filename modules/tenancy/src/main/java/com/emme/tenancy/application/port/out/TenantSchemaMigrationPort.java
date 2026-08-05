package com.emme.tenancy.application.port.out;

import java.util.UUID;

/** Database capability for creating and migrating a tenant schema. */
public interface TenantSchemaMigrationPort {

  String migrate(UUID tenantId, String slug);
}
