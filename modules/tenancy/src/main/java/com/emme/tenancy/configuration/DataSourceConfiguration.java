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
@ConditionalOnExpression(
    "'${spring.datasource.core.url:}' != '' && !'${spring.datasource.core.url:}'.contains('h2')")
public class DataSourceConfiguration {

  @Bean(name = "coreDataSource")
  @Primary
  DataSource coreDataSource(
      @Value("${spring.datasource.core.url}") String url,
      @Value("${spring.datasource.core.username}") String username,
      @Value("${spring.datasource.core.password}") String password,
      @Value("${spring.datasource.core.hikari.maximum-pool-size:5}") int maxPoolSize,
      @Value("${spring.datasource.core.hikari.minimum-idle:2}") int minIdle,
      @Value("${spring.datasource.core.hikari.pool-name:emme-core-pool}") String poolName) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(url);
    config.setUsername(username);
    config.setPassword(password);
    config.setMaximumPoolSize(maxPoolSize);
    config.setMinimumIdle(minIdle);
    config.setPoolName(poolName);
    config.setConnectionInitSql("SET search_path TO emme_core, public");
    return new HikariDataSource(config);
  }
}
