package com.emme.tenancy.configuration;

import com.emme.tenancy.adapter.out.client.database.TenantRoutingDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Replaces Spring Boot's auto-configured DataSource with our {@link TenantRoutingDataSource}.
 *
 * <p>This bean becomes the {@code @Primary} {@link DataSource}, which causes {@code
 * DataSourceAutoConfiguration} to skip creating its own DataSource via its
 * {@code @ConditionalOnMissingBean(DataSource.class)} guard.
 */
@Configuration
@ConditionalOnExpression("!'${spring.datasource.url:}'.contains('h2')")
public class DataSourceConfiguration {

  /**
   * Exposes the existing {@link TenantRoutingDataSource} component as the primary {@link
   * DataSource} bean.
   *
   * <p>The {@code TenantRoutingDataSource} is already a {@code @Component} — this method wraps it
   * so it is registered as a {@code DataSource} bean named {@code dataSource} (matching the
   * auto-config name), which prevents Spring Boot from creating its own HikariCP pool.
   */
  @Bean
  @Primary
  public DataSource dataSource(TenantRoutingDataSource tenantRoutingDataSource) {
    return tenantRoutingDataSource;
  }
}
