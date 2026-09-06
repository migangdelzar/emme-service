package com.emme.assistant.ai.application.trace;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiSemanticExecutionTraceTest {

  @Test
  void rejectsUnboundedMatchMetadata() {
    assertThatThrownBy(
            () ->
                new AiSemanticExecutionTrace(
                    UUID.randomUUID(),
                    null,
                    null,
                    "routing",
                    "accepted",
                    0.9,
                    0.7,
                    0.2,
                    java.util.stream.IntStream.range(0, 17)
                        .mapToObj(index -> "MATCH_" + index)
                        .toList(),
                    null,
                    null,
                    null,
                    10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("matches must contain at most 16 entries");
  }
}
