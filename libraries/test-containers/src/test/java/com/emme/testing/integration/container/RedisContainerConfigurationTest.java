package com.emme.testing.integration.container;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

class RedisContainerConfigurationTest {

  @Test
  void createsDisposableContainersForIsolatedIntegrationRuns() {
    GenericContainer<?> container = new RedisContainerConfiguration().redisContainer();

    assertThat(container.isShouldBeReused()).isFalse();
  }
}
