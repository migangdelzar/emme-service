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
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
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
    AiChatCompletion local = mock(AiChatCompletion.class);
    AiChatCompletion cloud = mock(AiChatCompletion.class);
    when(local.complete(org.mockito.ArgumentMatchers.any()))
        .thenReturn(response("hola", "local", "llama-local"));
    ChatModelSelector selector =
        new ChatModelSelector(
            List.of(
                new ChatModelSelector.Provider("local", local, "llama-local"),
                new ChatModelSelector.Provider("cloud", cloud, "gpt-cloud")));
    AiExecutionContext context = context();

    ChatResponse response =
        call(
            context,
            selector,
            request("context", "hello", context, List.of("local", "cloud"), true));

    assertThat(response).isEqualTo(new ChatResponse("hola", "local", "llama-local", 0, 0));
    verifyNoInteractions(cloud);
  }

  @Test
  void canonicalRequestsWithoutFallbackDoNotTryTheNextAdmittedProvider() {
    AiChatCompletion local = mock(AiChatCompletion.class);
    AiChatCompletion cloud = mock(AiChatCompletion.class);
    when(local.complete(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new ChatProviderUnavailableException("local unavailable"));
    ChatModelSelector selector =
        new ChatModelSelector(
            List.of(
                new ChatModelSelector.Provider("local", local, "llama-local"),
                new ChatModelSelector.Provider("cloud", cloud, "gpt-cloud")));
    AiExecutionContext context = context();

    assertThatThrownBy(
            () ->
                call(
                    context,
                    selector,
                    request("", "hello", context, List.of("local", "cloud"), false)))
        .isInstanceOf(ChatProviderUnavailableException.class);
    verifyNoInteractions(cloud);
  }

  @Test
  void returnsTheFirstHealthyProviderResponse() {
    AiChatCompletion local = mock(AiChatCompletion.class);
    AiChatCompletion cloud = mock(AiChatCompletion.class);
    when(local.complete(org.mockito.ArgumentMatchers.any()))
        .thenReturn(response("hola", "local", "unknown-model"));
    ChatModelSelector selector =
        new ChatModelSelector(
            List.of(
                new ChatModelSelector.Provider("local", local),
                new ChatModelSelector.Provider("cloud", cloud)));
    AiExecutionContext context = context();

    assertThat(
            call(context, selector, request("", "hello", context, List.of("local", "cloud"), true)))
        .isEqualTo(response("hola", "local", "unknown-model"));
    verifyNoInteractions(cloud);
  }

  @Test
  void fallsBackOnlyWhenTheCurrentProviderIsUnavailable() {
    AiChatCompletion local = mock(AiChatCompletion.class);
    AiChatCompletion cloud = mock(AiChatCompletion.class);
    when(local.complete(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new ChatProviderUnavailableException("local unavailable"));
    when(cloud.complete(org.mockito.ArgumentMatchers.any()))
        .thenReturn(response("hola", "cloud", "unknown-model"));
    ChatModelSelector selector =
        new ChatModelSelector(
            List.of(
                new ChatModelSelector.Provider("local", local),
                new ChatModelSelector.Provider("cloud", cloud)));
    AiExecutionContext context = context();

    assertThat(
            call(context, selector, request("", "hello", context, List.of("local", "cloud"), true)))
        .isEqualTo(response("hola", "cloud", "unknown-model"));
    var invocationOrder = inOrder(local, cloud);
    invocationOrder.verify(local).complete(org.mockito.ArgumentMatchers.any());
    invocationOrder.verify(cloud).complete(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void reportsWhenEveryProviderIsUnavailable() {
    AiChatCompletion local = mock(AiChatCompletion.class);
    when(local.complete(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new ChatProviderUnavailableException("local unavailable"));
    ChatModelSelector selector =
        new ChatModelSelector(List.of(new ChatModelSelector.Provider("local", local)));
    AiExecutionContext context = context();

    assertThatThrownBy(
            () -> call(context, selector, request("", "hello", context, List.of("local"), true)))
        .isInstanceOf(ChatProviderUnavailableException.class)
        .hasMessage("All configured chat providers are unavailable: local");
  }

  @Test
  void admitsEachProviderAttemptThroughTheExistingModelScheduler() {
    AiChatCompletion local = mock(AiChatCompletion.class);
    when(local.complete(org.mockito.ArgumentMatchers.any()))
        .thenReturn(response("hola", "local", "unknown-model"));
    var scheduler = new RecordingScheduler();
    ChatModelSelector selector =
        new ChatModelSelector(
            List.of(new ChatModelSelector.Provider("local", local)),
            scheduler,
            Duration.ofSeconds(1));
    AiExecutionContext context = context();

    ChatResponse response =
        call(context, selector, request("", "hello", context, List.of("local"), true));

    assertThat(response.content()).isEqualTo("hola");
    assertThat(scheduler.capabilities).containsExactly(ModelCapability.GENERATION);
  }

  @Test
  void reportsTheProviderAndModelThatProducedTheResponse() {
    AiChatCompletion local = mock(AiChatCompletion.class);
    when(local.complete(org.mockito.ArgumentMatchers.any()))
        .thenReturn(response("hola", "local", "ignored-by-selector"));
    ChatModelSelector selector =
        new ChatModelSelector(List.of(new ChatModelSelector.Provider("local", local, "llama-3")));
    AiExecutionContext context = context();

    ChatResponse result =
        call(context, selector, request("", "hello", context, List.of("local"), true));

    assertThat(result).isEqualTo(new ChatResponse("hola", "local", "llama-3", 0, 0));
  }

  @Test
  void preservesFallbackIdentityAndAdmissionForEachAttempt() {
    AiChatCompletion primary = mock(AiChatCompletion.class);
    AiChatCompletion fallback = mock(AiChatCompletion.class);
    when(primary.complete(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new ChatProviderUnavailableException("local unavailable"));
    when(fallback.complete(org.mockito.ArgumentMatchers.any()))
        .thenReturn(response("hola", "cloud", "ignored-by-selector"));
    var scheduler = new RecordingScheduler();
    ChatModelSelector selector =
        new ChatModelSelector(
            List.of(
                new ChatModelSelector.Provider("local", primary, "llama-local"),
                new ChatModelSelector.Provider("cloud", fallback, "gpt-cloud")),
            scheduler,
            Duration.ofSeconds(1));
    AiExecutionContext context = context();

    ChatResponse result =
        call(context, selector, request("", "hello", context, List.of("local", "cloud"), true));

    assertThat(result).isEqualTo(new ChatResponse("hola", "cloud", "gpt-cloud", 0, 0));
    assertThat(scheduler.capabilities)
        .containsExactly(ModelCapability.GENERATION, ModelCapability.GENERATION);
    var invocationOrder = inOrder(primary, fallback);
    invocationOrder.verify(primary).complete(org.mockito.ArgumentMatchers.any());
    invocationOrder.verify(fallback).complete(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void propagatesNonFallbackChatFailuresWithoutTryingAnotherModel() {
    AiChatCompletion primary = mock(AiChatCompletion.class);
    AiChatCompletion fallback = mock(AiChatCompletion.class);
    IllegalArgumentException invalidRequest = new IllegalArgumentException("invalid request");
    when(primary.complete(org.mockito.ArgumentMatchers.any())).thenThrow(invalidRequest);
    ChatModelSelector selector =
        new ChatModelSelector(
            List.of(
                new ChatModelSelector.Provider("local", primary),
                new ChatModelSelector.Provider("cloud", fallback)));
    AiExecutionContext context = context();

    assertThatThrownBy(
            () ->
                call(
                    context,
                    selector,
                    request("", "hello", context, List.of("local", "cloud"), true)))
        .isSameAs(invalidRequest);
    verifyNoInteractions(fallback);
  }

  private static ChatResponse call(
      AiExecutionContext context, ChatModelSelector selector, AiChatCompletion.Request request) {
    return AiExecutionContextScope.call(context, () -> selector.complete(request));
  }

  private static AiChatCompletion.Request request(
      String conversationContext,
      String userMessage,
      AiExecutionContext context,
      List<String> providers,
      boolean fallbackAllowed) {
    return new AiChatCompletion.Request(
        conversationContext,
        userMessage,
        context,
        new AiChatCompletion.ProviderPolicy(providers, fallbackAllowed));
  }

  private static ChatResponse response(String content, String provider, String model) {
    return new ChatResponse(content, provider, model, 0, 0);
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
