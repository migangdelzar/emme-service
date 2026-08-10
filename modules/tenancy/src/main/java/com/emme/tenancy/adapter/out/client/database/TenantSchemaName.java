package com.emme.tenancy.adapter.out.client.database;

/** Validates tenant schema identifiers before they are used in database statements. */
public final class TenantSchemaName {

  private TenantSchemaName() {}

  public static String fromSlug(String slug) {
    String schemaName = slug.replace("-", "_").replaceAll("[^a-z0-9_]", "_");
    return requireValid(schemaName);
  }

  public static String requireValid(String schemaName) {
    if (schemaName == null || !schemaName.matches("[a-z0-9_]+")) {
      throw new IllegalArgumentException("Invalid tenant schema name: " + schemaName);
    }
    return schemaName;
  }
}
