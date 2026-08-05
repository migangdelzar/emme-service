package com.emme.tenancy.adapter.out.client.database;

import com.emme.shared.persistence.jdbc.JdbcConnectionExecutor;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import java.sql.Statement;
import java.util.UUID;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.stereotype.Component;

/** Creates tenant schemas and applies the Studio Liquibase changelog. */
@Component
public final class LiquibaseTenantSchemaMigrationAdapter implements TenantSchemaMigrationPort {

  private static final String STUDIO_CHANGELOG = "db/emme-studio/changelog.yaml";

  private final JdbcConnectionExecutor connectionExecutor;

  public LiquibaseTenantSchemaMigrationAdapter(JdbcConnectionExecutor connectionExecutor) {
    this.connectionExecutor = connectionExecutor;
  }

  @Override
  public String migrate(UUID tenantId, String slug) {
    String schemaName = TenantSchemaName.fromSlug(slug);
    try {
      connectionExecutor.consumeWithConnection(
          connection -> {
            try (Statement statement = connection.createStatement()) {
              statement.execute("CREATE SCHEMA IF NOT EXISTS \"" + schemaName + "\"");
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
              liquibase.update("dev");
            }
          });
      return schemaName;
    } catch (RuntimeException exception) {
      throw new IllegalStateException("Failed to migrate tenant schema: " + schemaName, exception);
    }
  }
}
