package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class AiToolIdempotencyPropertiesTest {

  @Test
  void defaultsToABoundedRecoveryLease() {
    assertThat(new AiToolIdempotencyProperties(null).claimLease())
        .isEqualTo(Duration.ofMinutes(15));
  }

  @Test
  void rejectsAnUnboundedRecoveryLease() {
    assertThatThrownBy(() -> new AiToolIdempotencyProperties(Duration.ofDays(2)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("claimLease must not exceed one day");
  }
}
