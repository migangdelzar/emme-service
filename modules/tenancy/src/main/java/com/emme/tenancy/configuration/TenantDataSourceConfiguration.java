package com.emme.tenancy.configuration;

import com.emme.tenancy.adapter.out.client.database.TenantIdentifierResolver;
import com.emme.tenancy.adapter.out.client.database.TenantScopedDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Exposes tenant schema scoping as a stable DataSource boundary for consuming modules. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(name = "tenantRoutingDataSource")
public class TenantDataSourceConfiguration {

  @Bean(name = "tenantScopedDataSource")
  @ConditionalOnMissingBean(name = "tenantScopedDataSource")
  DataSource tenantScopedDataSource(
      @Qualifier("tenantRoutingDataSource") DataSource routingDataSource,
      @Qualifier("bootstrapJdbcClient") JdbcClient bootstrapJdbc) {
    return new TenantScopedDataSource(
        routingDataSource, new TenantIdentifierResolver(bootstrapJdbc));
  }
}
