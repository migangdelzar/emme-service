package com.emme.tenancy.application.port.out;

/** Database capability for creating and migrating a tenant schema. */
public interface TenantSchemaMigrationPort {

  void migrate(String schemaName);
}
