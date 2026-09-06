package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.contracts.model.ChatResponse;
import com.emme.ai.contracts.model.ModelCapability;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import com.emme.assistant.ai.application.port.out.IdentifiedChatCompletionPort;
import com.emme.assistant.ai.application.provider.ChatModelSelector;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;

class ChatModelSelectorTest {

  @Test
  void completesCanonicalRequestsUsingTheAdmittedProviderOrder() {
    ChatCompletionPort local = mock(ChatCompletionPort.class);
    ChatCompletionPort cloud = mock(ChatCompletionPort.class);
    when(local.complete("context", "hello")).thenReturn("hola");
    ChatModelSelector selector =
        new ChatModelSelector(
            List.of(
                new ChatModelSelector.Provider("local", local, "llama-local"),
                new ChatModelSelector.Provider("cloud", cloud, "gpt-cloud")));
    AiExecutionContext context = context();

    ChatResponse response =
        AiExecutionContextScope.call(
            context,
            () ->
                selector.complete(
                    new AiChatCompletion.Request(
                        "context",
                        "hello",
                        context,
                        new AiChatCompletion.ProviderPolicy(List.of("local", "cloud"), true))));

    assertThat(response).isEqualTo(new ChatResponse("hola", "local", "llama-local", 0, 0));
    verifyNoInteractions(cloud);
  }

  @Test
  void canonicalRequestsWithoutFallbackDoNotTryTheNextAdmittedProvider() {
    ChatCompletionPort local = mock(ChatCompletionPort.class);
    ChatCompletionPort cloud = mock(ChatCompletionPort.class);
    when(local.complete("", "hello"))
        .thenThrow(new ChatProviderUnavailableException("local unavailable"));
    ChatModelSelector selector =
        new ChatModelSelector(
            List.of(
                new ChatModelSelector.Provider("local", local, "llama-local"),
                new ChatModelSelector.Provider("cloud", cloud, "gpt-cloud")));
    AiExecutionContext context = context();

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    context,
                    () ->
                        selector.complete(
                            new AiChatCompletion.Request(
                                "",
                                "hello",
                                context,
                                new AiChatCompletion.ProviderPolicy(
                                    List.of("local", "cloud"), false)))))
        .isInstanceOf(ChatProviderUnavailableException.class);
    verifyNoInteractions(cloud);
  }

  @Test
  void returnsTheFirstHealthyProviderResponse() {
    ChatCompletionPort local = mock(ChatCompletionPort.class);
    ChatCompletionPort cloud = mock(ChatCompletionPort.class);
    when(local.complete("", "hello")).thenReturn("hola");
    ChatModelSelector chain =
        new ChatModelSelector(
            List.of(
                new ChatModelSelector.Provider("local", local),
                new ChatModelSelector.Provider("cloud", cloud)));

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
    ChatModelSelector chain =
        new ChatModelSelector(
            List.of(
                new ChatModelSelector.Provider("local", local),
                new ChatModelSelector.Provider("cloud", cloud)));

    assertThat(chain.complete("", "hello")).isEqualTo("hola");
    var invocationOrder = inOrder(local, cloud);
    invocationOrder.verify(local).complete("", "hello");
    invocationOrder.verify(cloud).complete("", "hello");
  }

  @Test
  void reportsWhenEveryProviderIsUnavailable() {
    ChatCompletionPort local = mock(ChatCompletionPort.class);
    when(local.complete("", "hello"))
        .thenThrow(new ChatProviderUnavailableException("local unavailable"));
    ChatModelSelector chain =
        new ChatModelSelector(List.of(new ChatModelSelector.Provider("local", local)));

    assertThatThrownBy(() -> chain.complete("", "hello"))
        .isInstanceOf(ChatProviderUnavailableException.class)
        .hasMessage("All configured chat providers are unavailable: local");
  }

  @Test
  void admitsEachProviderAttemptThroughTheExistingModelScheduler() {
    ChatCompletionPort local = mock(ChatCompletionPort.class);
    when(local.complete("", "hello")).thenReturn("hola");
    var scheduler = new RecordingScheduler();
    ChatModelSelector chain =
        new ChatModelSelector(
            List.of(new ChatModelSelector.Provider("local", local)),
            scheduler,
            Duration.ofSeconds(1));

    String response = AiExecutionContextScope.call(context(), () -> chain.complete("", "hello"));

    assertThat(response).isEqualTo("hola");
    assertThat(scheduler.capabilities).containsExactly(ModelCapability.GENERATION);
  }

  @Test
  void reportsTheProviderAndModelThatProducedTheResponse() {
    ChatCompletionPort local = mock(ChatCompletionPort.class);
    when(local.complete("", "hello")).thenReturn("hola");
    ChatModelSelector chain =
        new ChatModelSelector(List.of(new ChatModelSelector.Provider("local", local, "llama-3")));

    assertThat(chain.completeWithIdentity("", "hello"))
        .isEqualTo(
            new IdentifiedChatCompletionPort.ChatCompletionResult("hola", "local", "llama-3"));
  }

  @Test
  void preservesFallbackIdentityAndAdmissionForEachAttempt() {
    ChatCompletionPort primary = mock(ChatCompletionPort.class);
    ChatCompletionPort fallback = mock(ChatCompletionPort.class);
    when(primary.complete("", "hello"))
        .thenThrow(new ChatProviderUnavailableException("local unavailable"));
    when(fallback.complete("", "hello")).thenReturn("hola");
    var scheduler = new RecordingScheduler();
    ChatModelSelector selector =
        new ChatModelSelector(
            List.of(
                new ChatModelSelector.Provider("local", primary, "llama-local"),
                new ChatModelSelector.Provider("cloud", fallback, "gpt-cloud")),
            scheduler,
            Duration.ofSeconds(1));

    var result =
        AiExecutionContextScope.call(context(), () -> selector.completeWithIdentity("", "hello"));

    assertThat(result)
        .isEqualTo(
            new IdentifiedChatCompletionPort.ChatCompletionResult("hola", "cloud", "gpt-cloud"));
    assertThat(scheduler.capabilities)
        .containsExactly(ModelCapability.GENERATION, ModelCapability.GENERATION);
    var invocationOrder = inOrder(primary, fallback);
    invocationOrder.verify(primary).complete("", "hello");
    invocationOrder.verify(fallback).complete("", "hello");
  }

  @Test
  void propagatesNonFallbackChatFailuresWithoutTryingAnotherModel() {
    ChatCompletionPort primary = mock(ChatCompletionPort.class);
    ChatCompletionPort fallback = mock(ChatCompletionPort.class);
    IllegalArgumentException invalidRequest = new IllegalArgumentException("invalid request");
    when(primary.complete("", "hello")).thenThrow(invalidRequest);
    ChatModelSelector selector =
        new ChatModelSelector(
            List.of(
                new ChatModelSelector.Provider("local", primary),
                new ChatModelSelector.Provider("cloud", fallback)));

    assertThatThrownBy(() -> selector.complete("", "hello")).isSameAs(invalidRequest);
    verifyNoInteractions(fallback);
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
      } catch (RuntimeException exception) {
        throw exception;
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }
  }
}
