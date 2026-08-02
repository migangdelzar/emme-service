package com.emme.tenancy.adapter.out.client.database;

import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
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

  private final DataSource dataSource;

  public LiquibaseTenantSchemaMigrationAdapter(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void migrate(String schemaName) {
    String validatedSchemaName = TenantSchemaName.requireValid(schemaName);
    try (Connection connection = dataSource.getConnection()) {
      try (Statement statement = connection.createStatement()) {
        statement.execute("CREATE SCHEMA IF NOT EXISTS \"" + validatedSchemaName + "\"");
      }

      Database database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      database.setDefaultSchemaName(validatedSchemaName);
      database.setLiquibaseSchemaName(validatedSchemaName);
      try (Liquibase liquibase =
          new Liquibase(
              STUDIO_CHANGELOG,
              new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader()),
              database)) {
        liquibase.update("dev");
      }
    } catch (Exception exception) {
      throw new IllegalStateException(
          "Failed to migrate tenant schema: " + validatedSchemaName, exception);
    }
  }
}
