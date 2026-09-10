package com.emme.tenancy.adapter.out.client.database;

import com.emme.shared.persistence.jdbc.BootstrapConnectionExecutor;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import java.sql.Statement;
import java.util.UUID;
import liquibase.Liquibase;
import liquibase.change.AbstractSQLChange;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Creates tenant schemas and applies the Studio Liquibase changelog. */
@Component
@RequiredArgsConstructor
public final class LiquibaseTenantSchemaMigrationAdapter implements TenantSchemaMigrationPort {

  private static final String STUDIO_CHANGELOG = "db/emme-studio/changelog.yaml";

  private final BootstrapConnectionExecutor connectionExecutor;

  @Override
  public String migrate(UUID tenantId, String slug) {
    String schemaName = TenantSchemaName.fromSlug(slug);
    try {
      connectionExecutor.consumeWithConnection(
          connection -> {
            try (Statement statement = connection.createStatement()) {
              statement.execute("CREATE SCHEMA IF NOT EXISTS \"" + schemaName + "\"");
              statement.execute(
                  "SET search_path TO \"%s\", emme_core, public".formatted(schemaName));
            }

            Database database =
                DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName(schemaName);
            database.setLiquibaseSchemaName(schemaName);
            try (Liquibase liquibase =
                new Liquibase(
                    STUDIO_CHANGELOG,
                    new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader()),
                    database)) {
              enablePostgresDollarQuotedChanges(liquibase.getDatabaseChangeLog());
              liquibase.update("dev");
            }
          });
      return schemaName;
    } catch (RuntimeException exception) {
      throw new IllegalStateException("Failed to migrate tenant schema: " + schemaName, exception);
    }
  }

  private static void enablePostgresDollarQuotedChanges(
      liquibase.changelog.DatabaseChangeLog changeLog) {
    for (ChangeSet changeSet : changeLog.getChangeSets()) {
      changeSet.getChanges().stream()
          .filter(AbstractSQLChange.class::isInstance)
          .map(AbstractSQLChange.class::cast)
          .filter(change -> change.getSql() != null && change.getSql().contains("DO $$"))
          .forEach(change -> change.setSplitStatements(false));
    }
  }
}
