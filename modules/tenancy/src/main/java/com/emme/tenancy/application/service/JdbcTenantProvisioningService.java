package com.emme.tenancy.application.service;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** JDBC-backed implementation of the tenant provisioning application capability. */
@Service
@Transactional
class JdbcTenantProvisioningService implements TenantProvisioningService {

  private final JdbcTemplate jdbc;

  JdbcTenantProvisioningService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public UUID requestProvisioning(String slug, String name, String timeZone, String locale) {
    String schemaName = slug.replace('-', '_').replaceAll("[^a-z0-9_]", "_").toLowerCase();
    return jdbc.queryForObject(
        "INSERT INTO emme_core.tenant_registry (slug, schema_name, status) VALUES (?, ?, 'PROVISIONING') RETURNING tenant_id",
        UUID.class,
        slug,
        schemaName);
  }

  @Override
  public TenantStatus getStatus(UUID tenantId) {
    return jdbc.queryForObject(
        "SELECT status, schema_name, last_migrated_at, migration_error FROM emme_core.tenant_registry WHERE tenant_id = ?",
        (rs, rowNum) ->
            new TenantStatus(
                rs.getString("status"),
                rs.getString("schema_name"),
                rs.getTimestamp("last_migrated_at") != null
                    ? rs.getTimestamp("last_migrated_at").toInstant()
                    : null,
                rs.getString("migration_error")),
        tenantId);
  }
}
