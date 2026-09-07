package com.emme.assistant.ai.configuration;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Provides a tenant-schema-aware JDBC client for AI persistence adapters. */
@Configuration(proxyBeanMethods = false)
@Conditional(SpringAiTenantJdbcConfiguration.TenantJdbcClientCondition.class)
public class SpringAiTenantJdbcConfiguration {

  @Bean(name = "tenantJdbcClient")
  @Primary
  JdbcClient tenantJdbcClient(@Qualifier("tenantScopedDataSource") DataSource tenantDataSource) {
    return JdbcClient.create(tenantDataSource);
  }

  static class TenantJdbcClientCondition extends AnyNestedCondition {

    TenantJdbcClientCondition() {
      super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnExpression(
        "('${spring.datasource.url:}' != '' && !'${spring.datasource.url:}'.contains('h2')) || "
            + "('${spring.datasource.core.url:}' != '' && "
            + "!'${spring.datasource.core.url:}'.contains('h2'))")
    static class ExplicitPostgresDataSource {}

    @ConditionalOnBean(name = "postgresContainer")
    static class ServiceConnectionDataSource {}
  }
}
