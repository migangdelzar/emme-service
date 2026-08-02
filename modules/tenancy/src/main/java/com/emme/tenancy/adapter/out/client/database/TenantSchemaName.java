package com.emme.tenancy.adapter.out.client.database;

/** Validates tenant schema identifiers before they are used in database statements. */
final class TenantSchemaName {

  private TenantSchemaName() {}

  static String requireValid(String schemaName) {
    if (schemaName == null || !schemaName.matches("[a-z0-9_]+")) {
      throw new IllegalArgumentException("Invalid tenant schema name: " + schemaName);
    }
    return schemaName;
  }
}
