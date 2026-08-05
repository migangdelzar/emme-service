package com.emme.tenancy.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnExpression("!'${spring.datasource.url:}'.contains('h2')")
public class DataSourceConfiguration {

  @Bean(name = "coreDataSource")
  @Primary
  public DataSource coreDataSource(
      @Value("${spring.datasource.url}") String url,
      @Value("${spring.datasource.username}") String username,
      @Value("${spring.datasource.password}") String password) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(url);
    config.setUsername(username);
    config.setPassword(password);
    config.setMinimumIdle(2);
    config.setMaximumPoolSize(5);
    config.setConnectionInitSql("SET search_path TO emme_core, public");
    return new HikariDataSource(config);
  }
}
