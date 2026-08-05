package com.emme.tenancy.configuration;

import jakarta.validation.constraints.NotBlank;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConditionalOnExpression(
    "'${spring.datasource.url:}' != '' && !'${spring.datasource.url:}'.contains('h2')")
public class BootstrapJdbcConfiguration {

  @Bean(name = "bootstrapJdbcDataSource")
  DataSource bootstrapJdbcDataSource(BootstrapConnectionProperties props) {
    var dataSource = new DriverManagerDataSource();
    dataSource.setUrl(props.url());
    dataSource.setUsername(props.username());
    dataSource.setPassword(props.password());
    return dataSource;
  }

  @Bean(name = "bootstrapJdbcTemplate")
  JdbcTemplate bootstrapJdbcTemplate(@Qualifier("bootstrapJdbcDataSource") DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Validated
  @ConfigurationProperties(prefix = "spring.datasource")
  public record BootstrapConnectionProperties(
      @NotBlank String url,
      @NotBlank String username,
      @NotBlank String password) {}
}
