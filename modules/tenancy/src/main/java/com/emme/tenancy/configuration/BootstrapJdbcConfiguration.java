package com.emme.tenancy.configuration;

import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Composition-root wiring for the registry bootstrap connection boundary. */
@Configuration
@ConditionalOnExpression(
    "('${spring.datasource.url:}' != '' && !'${spring.datasource.url:}'.contains('h2')) || "
        + "('${spring.datasource.core.url:}' != '' && !'${spring.datasource.core.url:}'.contains('h2'))")
public class BootstrapJdbcConfiguration {

  @Bean(name = "bootstrapJdbcDataSource")
  DataSource bootstrapJdbcDataSource(
      @Value("${spring.datasource.url:}") String url,
      @Value("${spring.datasource.username:}") String username,
      @Value("${spring.datasource.password:}") String password,
      @Qualifier("coreDataSource") ObjectProvider<DataSource> coreDataSource) {
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
}
