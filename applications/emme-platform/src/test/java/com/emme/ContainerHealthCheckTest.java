package com.emme;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContainerHealthCheckTest {

  @Test
  void acceptsAnUpActuatorResponse() {
    assertThat(ContainerHealthCheck.isHealthy(200, "{ \"status\": \"UP\" }")).isTrue();
  }

  @Test
  void rejectsNonSuccessfulOrNonUpResponses() {
    assertThat(ContainerHealthCheck.isHealthy(503, "{\"status\":\"DOWN\"}")).isFalse();
    assertThat(ContainerHealthCheck.isHealthy(200, "{\"status\":\"DEGRADED\"}")).isFalse();
  }
}
