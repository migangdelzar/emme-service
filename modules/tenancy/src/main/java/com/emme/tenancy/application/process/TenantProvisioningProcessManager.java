package com.emme.tenancy.application.process;

import com.emme.functional.unchecked.UConsumer;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.kernel.tracing.CorrelationId;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Coordinates the long-running tenant schema provisioning process. */
@Component
final class TenantProvisioningProcessManager {

  private static final Logger log = LoggerFactory.getLogger(TenantProvisioningProcessManager.class);
  private static final String STUDIO_CHANGELOG = "db/emme-studio/changelog.yaml";

  private final JdbcTemplate jdbc;
  private final DataSource dataSource;

  TenantProvisioningProcessManager(JdbcTemplate jdbc, DataSource dataSource) {
    this.jdbc = jdbc;
    this.dataSource = dataSource;
  }

  @Scheduled(fixedDelayString = "${app.tenant.provisioning.poll-interval:PT10S}")
  @SchedulerLock(name = "tenant-provisioning", lockAtMostFor = "PT5M")
  public void processProvisioningRequests() {
    List<TenantRow> pending =
        jdbc.query(
            "SELECT tenant_id, slug, schema_name FROM emme_core.tenant_registry WHERE status = 'PROVISIONING'",
            (rs, rowNum) ->
                new TenantRow(
                    rs.getObject("tenant_id", UUID.class),
                    rs.getString("slug"),
                    rs.getString("schema_name")));

    if (pending.isEmpty()) return;

    log.info("Found {} tenants awaiting provisioning", pending.size());

    for (TenantRow row : pending) {
      TenantContextHolder.withTenantAndCorrelation(
          row.tenantId,
          CorrelationId.generate(),
          () -> {
            try {
              provisionTenant(row);
              jdbc.update(
                  "UPDATE emme_core.tenant_registry SET status = 'ACTIVE', schema_version = '0.1.0', last_migrated_at = now(), migration_error = NULL, updated_at = now() WHERE tenant_id = ?",
                  row.tenantId);
              log.info("Tenant {} (schema: {}) provisioned successfully", row.slug, row.schemaName);
            } catch (Exception e) {
              log.error("Failed to provision tenant {}: {}", row.slug, e.getMessage());
              jdbc.update(
                  "UPDATE emme_core.tenant_registry SET status = 'FAILED', migration_error = ?, updated_at = now() WHERE tenant_id = ?",
                  e.getMessage() != null
                      ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500))
                      : "Unknown error",
                  row.tenantId);
            }
            return null;
          });
    }
  }

  private void provisionTenant(TenantRow row) {
    String schema = row.schemaName;
    withConnection(
        connection -> {
          try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS \"" + schema + "\"");
          }
          Database database =
              DatabaseFactory.getInstance()
                  .findCorrectDatabaseImplementation(new JdbcConnection(connection));
          database.setDefaultSchemaName(schema);
          database.setLiquibaseSchemaName(schema);
          try (Liquibase liquibase =
              new Liquibase(
                  STUDIO_CHANGELOG,
                  new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader()),
                  database)) {
            liquibase.update("dev");
          }
        });
  }

  private void withConnection(UConsumer<Connection> action) {
    try (Connection connection = dataSource.getConnection()) {
      action.accept(connection);
    } catch (Exception e) {
      throw new RuntimeException("Failed to use tenant provisioning connection", e);
    }
  }

  private record TenantRow(UUID tenantId, String slug, String schemaName) {}
}
