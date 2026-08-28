package com.emme.assistant.ai.application.trace;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiExecutionTraceContractTest {

  @Test
  void rejectsNegativeTokenUsage() {
    assertThatThrownBy(
            () ->
                new AiModelExecutionTrace(
                    UUID.randomUUID(),
                    "CHAT_COMPLETION",
                    "local",
                    "model-v1",
                    "chat-v1",
                    null,
                    AiExecutionStatus.SUCCEEDED,
                    1,
                    -1,
                    2,
                    3,
                    null,
                    "request",
                    "response",
                    null,
                    null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("inputTokens must not be negative");
  }

  @Test
  void requiresAResponseForSuccessfulModelExecution() {
    assertThatThrownBy(
            () ->
                new AiModelExecutionTrace(
                    UUID.randomUUID(),
                    "CHAT_COMPLETION",
                    "local",
                    "model-v1",
                    "chat-v1",
                    null,
                    AiExecutionStatus.SUCCEEDED,
                    1,
                    null,
                    null,
                    null,
                    null,
                    "request",
                    " ",
                    null,
                    null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("successful model execution requires a response payload");
  }
}
