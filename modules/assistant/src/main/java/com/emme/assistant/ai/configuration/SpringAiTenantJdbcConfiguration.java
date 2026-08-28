package com.emme.assistant.ai.configuration;

import com.emme.tenancy.adapter.out.client.database.TenantIdentifierResolver;
import com.emme.tenancy.adapter.out.client.database.TenantRoutingDataSource;
import com.emme.tenancy.adapter.out.client.database.TenantScopedDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Provides a tenant-schema-aware JDBC client for AI persistence adapters. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(TenantRoutingDataSource.class)
public class SpringAiTenantJdbcConfiguration {

  @Bean(name = "aiTenantJdbcClient")
  @Primary
  JdbcClient aiTenantJdbcClient(@Qualifier("tenantRoutingDataSource") DataSource tenantDataSource) {
    return JdbcClient.create(
        new TenantScopedDataSource(tenantDataSource, new TenantIdentifierResolver()));
  }
}
