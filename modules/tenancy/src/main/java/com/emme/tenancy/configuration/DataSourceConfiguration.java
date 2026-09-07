package com.emme.tenancy.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.context.annotation.Primary;

@Configuration
@Conditional(DataSourceConfiguration.CoreDataSourceCondition.class)
public class DataSourceConfiguration {

  @Bean(name = "coreDataSource")
  @Primary
  DataSource coreDataSource(
      ObjectProvider<JdbcConnectionDetails> jdbcConnectionDetails,
      @Value("${spring.datasource.core.url:${spring.datasource.url:}}") String url,
      @Value("${spring.datasource.core.username:${spring.datasource.username:}}") String username,
      @Value("${spring.datasource.core.password:${spring.datasource.password:}}") String password,
      @Value("${spring.datasource.core.hikari.maximum-pool-size:5}") int maxPoolSize,
      @Value("${spring.datasource.core.hikari.minimum-idle:2}") int minIdle,
      @Value("${spring.datasource.core.hikari.pool-name:emme-core-pool}") String poolName) {
    HikariConfig config = new HikariConfig();
    var connectionDetails = jdbcConnectionDetails.getIfAvailable();
    config.setJdbcUrl(connectionDetails == null ? url : connectionDetails.getJdbcUrl());
    config.setUsername(connectionDetails == null ? username : connectionDetails.getUsername());
    config.setPassword(connectionDetails == null ? password : connectionDetails.getPassword());
    config.setMaximumPoolSize(maxPoolSize);
    config.setMinimumIdle(minIdle);
    config.setPoolName(poolName);
    config.setConnectionInitSql("SET search_path TO emme_core, public");
    return new HikariDataSource(config);
  }

  static class CoreDataSourceCondition extends AnyNestedCondition {

    CoreDataSourceCondition() {
      super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnExpression(
        "'${spring.datasource.core.url:}' != '' && !'${spring.datasource.core.url:}'.contains('h2')")
    static class ExplicitCoreDataSource {}

    @ConditionalOnBean(name = "postgresContainer")
    static class ServiceConnectionDataSource {}
  }
}
