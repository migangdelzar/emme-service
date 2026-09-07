package com.emme.tenancy.configuration;

import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Composition-root wiring for the registry bootstrap connection boundary. */
@Configuration
@Conditional(BootstrapJdbcConfiguration.BootstrapDataSourceCondition.class)
public class BootstrapJdbcConfiguration {

  @Bean(name = "bootstrapJdbcDataSource")
  DataSource bootstrapJdbcDataSource(
      @Value("${spring.datasource.url:}") String url,
      @Value("${spring.datasource.username:}") String username,
      @Value("${spring.datasource.password:}") String password,
      ObjectProvider<JdbcConnectionDetails> jdbcConnectionDetails,
      @Qualifier("coreDataSource") ObjectProvider<DataSource> coreDataSource) {
    var connectionDetails = jdbcConnectionDetails.getIfAvailable();
    if (connectionDetails != null) {
      var dataSource = new DriverManagerDataSource();
      dataSource.setUrl(connectionDetails.getJdbcUrl());
      dataSource.setUsername(connectionDetails.getUsername());
      dataSource.setPassword(connectionDetails.getPassword());
      return dataSource;
    }
    if (url.isBlank()) {
      return coreDataSource.getIfAvailable(
          () -> {
            throw new IllegalStateException(
                "No bootstrap JDBC URL or core DataSource is configured");
          });
    }
    var dataSource = new DriverManagerDataSource();
    dataSource.setUrl(url);
    dataSource.setUsername(username);
    dataSource.setPassword(password);
    return dataSource;
  }

  @Bean(name = "bootstrapJdbcTemplate")
  JdbcTemplate bootstrapJdbcTemplate(@Qualifier("bootstrapJdbcDataSource") DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Bean(name = "bootstrapJdbcClient")
  JdbcClient bootstrapJdbcClient(@Qualifier("bootstrapJdbcDataSource") DataSource dataSource) {
    return JdbcClient.create(dataSource);
  }

  static class BootstrapDataSourceCondition extends AnyNestedCondition {

    BootstrapDataSourceCondition() {
      super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnExpression(
        "('${spring.datasource.url:}' != '' && !'${spring.datasource.url:}'.contains('h2')) || "
            + "('${spring.datasource.core.url:}' != '' && !'${spring.datasource.core.url:}'.contains('h2'))")
    static class ExplicitBootstrapDataSource {}

    @ConditionalOnBean(name = "postgresContainer")
    static class ServiceConnectionDataSource {}
  }
}
