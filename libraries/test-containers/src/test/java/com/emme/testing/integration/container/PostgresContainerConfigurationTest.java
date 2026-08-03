package com.emme.testing.integration.container;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

class PostgresContainerConfigurationTest {

  @Test
  void createsNonReusableContainersForIsolatedIntegrationRuns() {
    PostgreSQLContainer<?> container = new PostgresContainerConfiguration().postgresContainer();

    assertThat(container.isShouldBeReused()).isFalse();
  }
}
