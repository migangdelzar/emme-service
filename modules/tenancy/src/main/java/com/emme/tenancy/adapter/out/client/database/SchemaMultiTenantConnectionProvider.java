package com.emme.tenancy.adapter.out.client.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(name = "tenantRoutingDataSource")
@SuppressWarnings("serial")
public class SchemaMultiTenantConnectionProvider
    implements MultiTenantConnectionProvider<String>, HibernatePropertiesCustomizer {

  private static final Logger log =
      LoggerFactory.getLogger(SchemaMultiTenantConnectionProvider.class);
  private static final String CORE_SCHEMA = "emme_core";

  private final DataSource metadataDataSource;
  private final TenantDatabasePoolProvider tenantPools;

  public SchemaMultiTenantConnectionProvider(
      DataSource metadataDataSource, TenantDatabasePoolProvider tenantPools) {
    this.metadataDataSource = metadataDataSource;
    this.tenantPools = tenantPools;
  }

  @Override
  public Connection getConnection(String tenantIdentifier) throws SQLException {
    Connection connection;
    if (CORE_SCHEMA.equals(tenantIdentifier)) {
      connection = metadataDataSource.getConnection();
    } else {
      String schema = TenantSchemaName.requireValid(tenantIdentifier);
      connection = tenantPools.getDataSource().getConnection();
      connection.setSchema(schema);
      log.debug("Connection routed to schema {}", schema);
    }
    return connection;
  }

  @Override
  public void releaseConnection(String tenantIdentifier, Connection connection)
      throws SQLException {
    try {
      connection.setSchema(CORE_SCHEMA);
    } finally {
      connection.close();
    }
  }

  @Override
  public Connection getAnyConnection() throws SQLException {
    return metadataDataSource.getConnection();
  }

  @Override
  public void releaseAnyConnection(Connection connection) throws SQLException {
    connection.close();
  }

  @Override
  public boolean supportsAggressiveRelease() {
    return false;
  }

  @Override
  public void customize(Map<String, Object> hibernateProperties) {
    hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, this);
  }

  @Override
  public boolean isUnwrappableAs(Class<?> unwrapType) {
    return false;
  }

  @Override
  public <T> T unwrap(Class<T> unwrapType) {
    throw new UnknownUnwrapTypeException(unwrapType);
  }
}
