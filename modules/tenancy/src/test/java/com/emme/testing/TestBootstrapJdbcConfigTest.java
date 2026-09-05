package com.emme.testing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TestBootstrapJdbcConfigTest {

  @Test
  void usesAStableNoOpMigrationPortForH2ModuleTests() {
    String slug = "test_tenant";

    assertThat(
            new TestBootstrapJdbcConfig()
                .tenantSchemaMigrationPort()
                .migrate(UUID.randomUUID(), slug))
        .isEqualTo(slug);
  }
}
