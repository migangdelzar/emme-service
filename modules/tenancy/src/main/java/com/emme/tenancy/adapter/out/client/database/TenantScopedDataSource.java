package com.emme.tenancy.adapter.out.client.database;

import com.emme.kernel.context.TenantContextHolder;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * Applies the authenticated tenant schema to every JDBC connection acquired from a delegate.
 *
 * <p>This adapter is intended for tenant-scoped JDBC clients. It fails closed when no tenant is
 * present, so a caller cannot accidentally use the default schema for tenant data.
 */
public final class TenantScopedDataSource implements DataSource {

  private final DataSource delegate;
  private final TenantIdentifierResolver schemaResolver;

  public TenantScopedDataSource(DataSource delegate, TenantIdentifierResolver schemaResolver) {
    this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    this.schemaResolver = Objects.requireNonNull(schemaResolver, "schemaResolver must not be null");
  }

  @Override
  public Connection getConnection() throws SQLException {
    String schema = currentSchema();
    return scoped(delegate.getConnection(), schema);
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    String schema = currentSchema();
    return scoped(delegate.getConnection(username, password), schema);
  }

  private String currentSchema() {
    TenantContextHolder.requireCurrentTenantId();
    return TenantSchemaName.requireValid(schemaResolver.resolveCurrentTenantIdentifier());
  }

  private Connection scoped(Connection connection, String schema) throws SQLException {
    try {
      connection.setSchema(schema);
      try (Statement statement = connection.createStatement()) {
        statement.execute("SET search_path TO %s, emme_core, public".formatted(schema));
      }
      return connection;
    } catch (RuntimeException | SQLException exception) {
      try {
        connection.close();
      } catch (SQLException closeException) {
        exception.addSuppressed(closeException);
      }
      throw exception;
    }
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return delegate.getLogWriter();
  }

  @Override
  public void setLogWriter(PrintWriter out) throws SQLException {
    delegate.setLogWriter(out);
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    delegate.setLoginTimeout(seconds);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return delegate.getLoginTimeout();
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return delegate.getParentLogger();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    return delegate.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return delegate.isWrapperFor(iface);
  }
}
