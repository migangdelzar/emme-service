package com.emme.ai.platform.adapter.out.provider.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.contracts.model.ChatResponse;
import com.emme.ai.platform.configuration.AiProviderProperties;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MockModelProviderTest {

  private final MockModelProvider provider =
      new MockModelProvider(new AiProviderProperties("mock", null, null, false));

  @Test
  void rejectsChatWhenNoAiExecutionContextIsBound() {
    assertThatThrownBy(() -> provider.chat("context", "hello"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  @Test
  void rejectsEmbeddingWhenNoAiExecutionContextIsBound() {
    assertThatThrownBy(() -> provider.embed("hello"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  @Test
  void providesDeterministicCapabilitiesInsideAnAiExecutionContext() {
    AiExecutionContext context =
        new AiExecutionContext(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Set.of("ROLE_CLIENT"),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "trace-mock",
            "idempotency-mock");

    assertThat(AiExecutionContextScope.call(context, () -> provider.chat("context", "hello")))
        .contains("hello");
    assertThat(AiExecutionContextScope.call(context, () -> provider.embed("hello")))
        .hasSize(providerEmbeddingDimension());
  }

  @Test
  void providesTheCanonicalChatCapabilityWithProviderIdentity() {
    AiExecutionContext context = context();

    ChatResponse response =
        AiExecutionContextScope.call(
            context,
            () ->
                ((AiChatCompletion) provider)
                    .complete(
                        new AiChatCompletion.Request(
                            "context",
                            "hello",
                            context,
                            new AiChatCompletion.ProviderPolicy(
                                java.util.List.of("mock"), false))));

    assertThat(response.content()).contains("hello");
    assertThat(response.provider()).isEqualTo("mock");
    assertThat(response.modelVersion()).isEqualTo("mock-v1");
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("ROLE_CLIENT"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-mock",
        "idempotency-mock");
  }

  private int providerEmbeddingDimension() {
    return AiProviderProperties.DEFAULT_EMBEDDING_DIMENSION;
  }
}
