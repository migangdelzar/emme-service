package com.emme.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.kernel.tracing.CorrelationContextHolder;
import com.emme.kernel.tracing.CorrelationId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class CorrelationContextHolderTest {

  @AfterEach
  void tearDown() {
    CorrelationId.clear();
    MDC.clear();
  }

  @Test
  void withCorrelationIdSetsMdcAndRestoresPreviousContext() {
    CorrelationId.set("previous");
    MDC.put("correlationId", "previous");

    String result =
        CorrelationContextHolder.withCorrelationId(
            "next",
            () -> {
              assertThat(CorrelationId.get()).isEqualTo("next");
              assertThat(MDC.get("correlationId")).isEqualTo("next");
              return "ok";
            });

    assertThat(result).isEqualTo("ok");
    assertThat(CorrelationId.get()).isEqualTo("previous");
    assertThat(MDC.get("correlationId")).isEqualTo("previous");
  }

  @Test
  void withGeneratedCorrelationIdCreatesARequiredId() {
    CorrelationContextHolder.withGeneratedCorrelationId(
        () -> {
          assertThat(CorrelationContextHolder.requireCorrelationId()).isNotBlank();
          return null;
        });
  }

  @Test
  void requireCorrelationIdThrowsWhenMissing() {
    assertThatThrownBy(CorrelationContextHolder::requireCorrelationId)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No correlation context");
  }
}
