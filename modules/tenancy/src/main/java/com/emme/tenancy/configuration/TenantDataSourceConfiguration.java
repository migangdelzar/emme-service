package com.emme.tenancy.configuration;

import com.emme.tenancy.adapter.out.client.database.TenantIdentifierResolver;
import com.emme.tenancy.adapter.out.client.database.TenantScopedDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;

/** Exposes tenant schema scoping as a stable DataSource boundary for consuming modules. */
@Configuration(proxyBeanMethods = false)
@Conditional(TenantDataSourceConfiguration.TenantDataSourceCondition.class)
public class TenantDataSourceConfiguration {

  @Bean(name = "tenantScopedDataSource")
  @ConditionalOnMissingBean(name = "tenantScopedDataSource")
  DataSource tenantScopedDataSource(
      @Qualifier("tenantRoutingDataSource") DataSource routingDataSource,
      TenantIdentifierResolver tenantIdentifierResolver) {
    return new TenantScopedDataSource(routingDataSource, tenantIdentifierResolver);
  }

  static class TenantDataSourceCondition extends AnyNestedCondition {

    TenantDataSourceCondition() {
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
