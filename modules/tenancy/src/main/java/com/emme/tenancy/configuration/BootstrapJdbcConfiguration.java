package com.emme.tenancy.configuration;

import com.emme.shared.persistence.jdbc.JdbcConnectionExecutor;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Composition-root wiring for the registry bootstrap connection boundary. */
@Configuration
@ConditionalOnProperty(name = "spring.datasource.url")
@ConditionalOnExpression("!'${spring.datasource.url:}'.contains('h2')")
public class BootstrapJdbcConfiguration {

  @Bean(name = "bootstrapJdbcDataSource")
  DataSource bootstrapJdbcDataSource(JdbcConnectionDetails connectionDetails) {
    var dataSource = new DriverManagerDataSource();
    dataSource.setUrl(connectionDetails.getJdbcUrl());
    dataSource.setUsername(connectionDetails.getUsername());
    dataSource.setPassword(connectionDetails.getPassword());
    return dataSource;
  }

  @Bean(name = "bootstrapJdbcTemplate")
  JdbcTemplate bootstrapJdbcTemplate(@Qualifier("bootstrapJdbcDataSource") DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Bean(name = "bootstrapJdbcConnectionExecutor")
  JdbcConnectionExecutor bootstrapJdbcConnectionExecutor(
      @Qualifier("bootstrapJdbcTemplate") JdbcTemplate jdbcTemplate) {
    return new JdbcConnectionExecutor(jdbcTemplate);
  }
}
