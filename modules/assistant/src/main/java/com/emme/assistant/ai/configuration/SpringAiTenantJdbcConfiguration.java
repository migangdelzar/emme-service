package com.emme.assistant.ai.configuration;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Provides a tenant-schema-aware JDBC client for AI persistence adapters. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(name = "tenantScopedDataSource")
public class SpringAiTenantJdbcConfiguration {

  @Bean(name = "aiTenantJdbcClient")
  @Primary
  JdbcClient aiTenantJdbcClient(@Qualifier("tenantScopedDataSource") DataSource tenantDataSource) {
    return JdbcClient.create(tenantDataSource);
  }
}
