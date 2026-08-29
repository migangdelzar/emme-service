package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.ModelCapability;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import com.emme.assistant.ai.application.provider.ChatProviderChain;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;

class ChatProviderChainTest {

  @Test
  void returnsTheFirstHealthyProviderResponse() {
    ChatCompletionPort local = mock(ChatCompletionPort.class);
    ChatCompletionPort cloud = mock(ChatCompletionPort.class);
    when(local.complete("", "hello")).thenReturn("hola");
    ChatProviderChain chain =
        new ChatProviderChain(
            List.of(
                new ChatProviderChain.Provider("local", local),
                new ChatProviderChain.Provider("cloud", cloud)));

    assertThat(chain.complete("", "hello")).isEqualTo("hola");
    verifyNoInteractions(cloud);
  }

  @Test
  void fallsBackOnlyWhenTheCurrentProviderIsUnavailable() {
    ChatCompletionPort local = mock(ChatCompletionPort.class);
    ChatCompletionPort cloud = mock(ChatCompletionPort.class);
    when(local.complete("", "hello"))
        .thenThrow(new ChatProviderUnavailableException("local unavailable"));
    when(cloud.complete("", "hello")).thenReturn("hola");
    ChatProviderChain chain =
        new ChatProviderChain(
            List.of(
                new ChatProviderChain.Provider("local", local),
                new ChatProviderChain.Provider("cloud", cloud)));

    assertThat(chain.complete("", "hello")).isEqualTo("hola");
  }

  @Test
  void reportsWhenEveryProviderIsUnavailable() {
    ChatCompletionPort local = mock(ChatCompletionPort.class);
    when(local.complete("", "hello"))
        .thenThrow(new ChatProviderUnavailableException("local unavailable"));
    ChatProviderChain chain =
        new ChatProviderChain(List.of(new ChatProviderChain.Provider("local", local)));

    assertThatThrownBy(() -> chain.complete("", "hello"))
        .isInstanceOf(ChatProviderUnavailableException.class)
        .hasMessage("All configured chat providers are unavailable: local");
  }

  @Test
  void admitsEachProviderAttemptThroughTheExistingModelScheduler() {
    ChatCompletionPort local = mock(ChatCompletionPort.class);
    when(local.complete("", "hello")).thenReturn("hola");
    var scheduler = new RecordingScheduler();
    ChatProviderChain chain =
        new ChatProviderChain(
            List.of(new ChatProviderChain.Provider("local", local)),
            scheduler,
            Duration.ofSeconds(1));

    String response = AiExecutionContextScope.call(context(), () -> chain.complete("", "hello"));

    assertThat(response).isEqualTo("hola");
    assertThat(scheduler.capabilities).containsExactly(ModelCapability.GENERATION);
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("CLIENT"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-chat",
        "idempotency-chat");
  }

  private static final class RecordingScheduler implements ModelExecutionScheduler {
    private final List<ModelCapability> capabilities = new java.util.ArrayList<>();

    @Override
    public <T> T execute(
        ModelCapability capability,
        AiExecutionContext context,
        Duration timeout,
        Callable<T> operation) {
      capabilities.add(capability);
      try {
        return operation.call();
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }
  }
}
