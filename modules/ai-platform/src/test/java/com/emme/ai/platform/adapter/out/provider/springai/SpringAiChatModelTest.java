package com.emme.ai.platform.adapter.out.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

class SpringAiChatModelTest {

  @Test
  void delegatesChatCompletionAndExposesTheConfiguredProviderIdentity() {
    ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    when(client
            .prompt()
            .system(org.mockito.ArgumentMatchers.anyString())
            .user("hello")
            .call()
            .content())
        .thenReturn(" Hola ");
    SpringAiChatModel model = new SpringAiChatModel(client, "ollama", "gemma-v1");

    assertThat(AiExecutionContextScope.call(context(), () -> model.complete("", "hello")))
        .isEqualTo("Hola");
    assertThat(model.provider()).isEqualTo("ollama");
    assertThat(model.modelVersion()).isEqualTo("gemma-v1");
  }

  @Test
  void propagatesProviderFailuresWithoutReproducingTransportHandling() {
    ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    RuntimeException failure = new RuntimeException("connection refused");
    when(client.prompt().system(org.mockito.ArgumentMatchers.anyString()).user("hello").call())
        .thenThrow(failure);
    SpringAiChatModel model = new SpringAiChatModel(client, "ollama", "gemma-v1");

    assertThatThrownBy(
            () -> AiExecutionContextScope.call(context(), () -> model.complete("", "hello")))
        .isSameAs(failure);
  }

  @Test
  void rejectsCompletionWhenTheBackendAiContextIsMissing() {
    ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    SpringAiChatModel model = new SpringAiChatModel(client, "ollama", "gemma-v1");

    assertThatThrownBy(() -> model.complete("", "hello"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("CLIENT"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-1",
        "idempotency-1");
  }
}
