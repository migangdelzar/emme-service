package com.emme.tenancy.configuration;

import com.emme.shared.persistence.jdbc.JdbcConnectionExecutor;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Composition-root wiring for the registry bootstrap connection boundary. */
@Configuration
@ConditionalOnExpression(
    "'${spring.datasource.core.url:}' != '' && !'${spring.datasource.core.url:}'.contains('h2')")
public class BootstrapJdbcConfiguration {

  @Bean(name = "bootstrapJdbcDataSource")
  DataSource bootstrapJdbcDataSource(
      @Value("${spring.datasource.core.url}") String url,
      @Value("${spring.datasource.core.username}") String username,
      @Value("${spring.datasource.core.password}") String password) {
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

  @Bean(name = "bootstrapJdbcConnectionExecutor")
  JdbcConnectionExecutor bootstrapJdbcConnectionExecutor(
      @Qualifier("bootstrapJdbcTemplate") JdbcTemplate jdbcTemplate) {
    return new JdbcConnectionExecutor(jdbcTemplate);
  }
}
