package com.emme.tenancy.adapter.out.client.database;

import com.emme.shared.persistence.jdbc.JdbcConnectionExecutor;
import com.emme.shared.persistence.jdbc.ThrowingSqlConnectionFunction;
import com.emme.tenancy.application.port.out.DatabaseRegistryEntry;
import com.emme.tenancy.application.port.out.DatabaseRegistryPort;
import com.emme.tenancy.configuration.TenantDatabaseConnectionProperties;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Bootstrap JDBC registry lookup through a dedicated, unpooled connection executor.
 *
 * <p>This deliberately bypasses the tenant-aware {@code DataSource} to break the circular
 * dependency between the entity manager, tenant routing, pool creation, and registry lookup.
 * Connection acquisition and cleanup remain owned by Spring's {@link
 * org.springframework.jdbc.core.JdbcTemplate}; this adapter only describes the query.
 */
@Component
public class DatabaseRegistryAdapter implements DatabaseRegistryPort {

  private static final UUID DEFAULT_DB_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
  private static final String REGISTRY_QUERY =
      "SELECT database_id, jdbc_url, name, min_pool_size, max_pool_size, priority, is_active "
          + "FROM emme_core.database_registry WHERE database_id = ?";

  private final String bootstrapUrl;
  private final Optional<JdbcConnectionExecutor> connectionExecutor;

  public DatabaseRegistryAdapter(
      TenantDatabaseConnectionProperties connectionProperties,
      Optional<JdbcConnectionExecutor> connectionExecutor) {
    this.bootstrapUrl = connectionProperties.getUrl();
    this.connectionExecutor = connectionExecutor;
  }

  @Override
  public Optional<DatabaseRegistryEntry> findById(UUID id) {
    if (DEFAULT_DB_ID.equals(id)) {
      return Optional.of(
          new DatabaseRegistryEntry(DEFAULT_DB_ID, "default", bootstrapUrl, 3, 20, 0, true));
    }

    return connectionExecutor
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Bootstrap JDBC connection executor is unavailable for tenant database lookup"))
        .withConnection(
            (ThrowingSqlConnectionFunction<Optional<DatabaseRegistryEntry>, SQLException>)
                connection -> {
                  try (var statement = connection.prepareStatement(REGISTRY_QUERY)) {
                    statement.setObject(1, id);
                    try (var resultSet = statement.executeQuery()) {
                      if (!resultSet.next()) {
                        return Optional.empty();
                      }
                      return Optional.of(
                          new DatabaseRegistryEntry(
                              UUID.fromString(resultSet.getString("database_id")),
                              resultSet.getString("name"),
                              resultSet.getString("jdbc_url"),
                              resultSet.getInt("min_pool_size"),
                              resultSet.getInt("max_pool_size"),
                              resultSet.getInt("priority"),
                              resultSet.getBoolean("is_active")));
                    }
                  }
                });
  }
}
