package com.emme.tenancy.service;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class DefaultTenantProvisioningService implements TenantProvisioningService {

  private final JdbcTemplate jdbc;

  DefaultTenantProvisioningService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public UUID requestProvisioning(String slug, String name, String timeZone, String locale) {
    String schemaName = slug.replace('-', '_').replaceAll("[^a-z0-9_]", "_").toLowerCase();
    UUID tenantId =
        jdbc.queryForObject(
            "INSERT INTO emme_core.tenant_registry (slug, schema_name, status) VALUES (?, ?, 'PROVISIONING') RETURNING tenant_id",
            UUID.class,
            slug,
            schemaName);
    return tenantId;
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
