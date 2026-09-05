package com.emme.testing;

import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/** Supplies H2-safe bootstrap boundaries for tenancy-owned module tests. */
@TestConfiguration
public class TestBootstrapJdbcConfig {

  @Bean(name = "bootstrapJdbcTemplate")
  JdbcTemplate bootstrapJdbcTemplate() {
    return Mockito.mock(JdbcTemplate.class);
  }

  @Bean
  @Primary
  public TenantSchemaMigrationPort tenantSchemaMigrationPort() {
    return (tenantId, slug) -> slug;
  }
}
