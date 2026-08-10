package com.emme.testing;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Supplies the bootstrap database boundary for module tests that exclude tenancy infrastructure.
 */
@TestConfiguration
public class TestBootstrapJdbcConfig {

  @Bean(name = "bootstrapJdbcTemplate")
  JdbcTemplate bootstrapJdbcTemplate() {
    return Mockito.mock(JdbcTemplate.class);
  }
}
