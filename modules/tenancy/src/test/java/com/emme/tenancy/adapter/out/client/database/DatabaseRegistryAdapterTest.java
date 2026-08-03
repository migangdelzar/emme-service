package com.emme.tenancy.adapter.out.client.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.shared.persistence.jdbc.JdbcConnectionExecutor;
import com.emme.shared.persistence.jdbc.ThrowingSqlConnectionFunction;
import com.emme.tenancy.application.port.out.DatabaseRegistryEntry;
import com.emme.tenancy.application.port.out.DatabaseRegistryPort;
import com.emme.tenancy.configuration.TenantDatabaseConnectionProperties;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;

class DatabaseRegistryAdapterTest {

  private static final UUID DEFAULT_DATABASE_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000000");

  @Test
  void exposesDefaultDatabaseThroughApplicationPortModel() {
    TenantDatabaseConnectionProperties connectionProperties = connectionProperties();
    DatabaseRegistryPort port =
        new DatabaseRegistryAdapter(
            connectionProperties,
            connectionDetails(),
            Optional.of(mock(JdbcConnectionExecutor.class)));

    assertThat(port.findById(DEFAULT_DATABASE_ID))
        .get()
        .isEqualTo(
            new DatabaseRegistryEntry(
                DEFAULT_DATABASE_ID, "default", "jdbc:h2:mem:bootstrap", 3, 20, 0, true));
  }

  @Test
  void performsTenantRegistryLookupThroughTheManagedConnectionExecutor() throws Exception {
    JdbcConnectionExecutor executor = mock(JdbcConnectionExecutor.class);
    Connection connection = mock(Connection.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);
    UUID databaseId = UUID.randomUUID();
    DatabaseRegistryEntry expected =
        new DatabaseRegistryEntry(databaseId, "tenant-db", "jdbc:h2:mem:tenant", 1, 5, 2, true);

    when(executor.withConnection(any()))
        .thenAnswer(
            invocation -> {
              ThrowingSqlConnectionFunction<?, ?> callback = invocation.getArgument(0);
              return callback.apply(connection);
            });
    when(connection.prepareStatement(any())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(resultSet.getString("database_id")).thenReturn(databaseId.toString());
    when(resultSet.getString("name")).thenReturn(expected.name());
    when(resultSet.getString("jdbc_url")).thenReturn(expected.jdbcUrl());
    when(resultSet.getInt("min_pool_size")).thenReturn(expected.minPoolSize());
    when(resultSet.getInt("max_pool_size")).thenReturn(expected.maxPoolSize());
    when(resultSet.getInt("priority")).thenReturn(expected.priority());
    when(resultSet.getBoolean("is_active")).thenReturn(expected.active());

    DatabaseRegistryPort port =
        new DatabaseRegistryAdapter(
            connectionProperties(), connectionDetails(), Optional.of(executor));

    assertThat(port.findById(databaseId)).contains(expected);
  }

  private static TenantDatabaseConnectionProperties connectionProperties() {
    TenantDatabaseConnectionProperties properties = new TenantDatabaseConnectionProperties();
    properties.setUrl("jdbc:h2:mem:bootstrap");
    properties.setUsername("emme");
    properties.setPassword("secret");
    return properties;
  }

  private static JdbcConnectionDetails connectionDetails() {
    return new JdbcConnectionDetails() {
      @Override
      public String getUsername() {
        return "emme";
      }

      @Override
      public String getPassword() {
        return "secret";
      }

      @Override
      public String getJdbcUrl() {
        return "jdbc:h2:mem:bootstrap";
      }
    };
  }
}
