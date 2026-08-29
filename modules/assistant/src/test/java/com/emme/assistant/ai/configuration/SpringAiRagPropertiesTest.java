package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SpringAiRagPropertiesTest {

  @Test
  void suppliesAConservativeRetrievalLimitWhenUnconfigured() {
    assertThat(new SpringAiRagProperties(true, 0).retrievalLimit()).isEqualTo(5);
  }

  @Test
  void rejectsAnUnsafeRetrievalLimit() {
    assertThatThrownBy(() -> new SpringAiRagProperties(true, 21))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("retrievalLimit must be between 1 and 20");
  }
}
