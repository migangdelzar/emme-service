package com.emme.tenancy.adapter.out.persistence.adapter;

import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Implements tenant provisioning registry lifecycle operations with JDBC. */
@Component
public final class JdbcTenantProvisioningRepository implements TenantProvisioningRepository {

  private final JdbcTemplate jdbc;

  public JdbcTenantProvisioningRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public UUID requestProvisioning(String slug, String schemaName) {
    return jdbc.queryForObject(
        "INSERT INTO emme_core.tenant_registry (slug, schema_name, status) VALUES (?, ?, 'PROVISIONING') RETURNING tenant_id",
        UUID.class,
        slug,
        schemaName);
  }

  @Override
  public TenantProvisioningStatus findStatus(UUID tenantId) {
    return jdbc.queryForObject(
        "SELECT status, schema_name, last_migrated_at, migration_error FROM emme_core.tenant_registry WHERE tenant_id = ?",
        (rs, rowNum) ->
            new TenantProvisioningStatus(
                rs.getString("status"),
                rs.getString("schema_name"),
                rs.getTimestamp("last_migrated_at") != null
                    ? rs.getTimestamp("last_migrated_at").toInstant()
                    : null,
                rs.getString("migration_error")),
        tenantId);
  }

  @Override
  public List<TenantProvisioningRequest> findPending() {
    return jdbc.query(
        "SELECT tenant_id, slug, schema_name FROM emme_core.tenant_registry WHERE status = 'PROVISIONING'",
        (rs, rowNum) ->
            new TenantProvisioningRequest(
                rs.getObject("tenant_id", UUID.class),
                rs.getString("slug"),
                rs.getString("schema_name")));
  }

  @Override
  public void markActive(UUID tenantId) {
    jdbc.update(
        "UPDATE emme_core.tenant_registry SET status = 'ACTIVE', schema_version = '0.1.0', last_migrated_at = now(), migration_error = NULL, updated_at = now() WHERE tenant_id = ?",
        tenantId);
  }

  @Override
  public void markFailed(UUID tenantId, String error) {
    jdbc.update(
        "UPDATE emme_core.tenant_registry SET status = 'FAILED', migration_error = ?, updated_at = now() WHERE tenant_id = ?",
        error,
        tenantId);
  }
}
