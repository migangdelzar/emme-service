package com.emme.assistant.ai.application.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.contracts.model.ChatResponse;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import com.emme.assistant.ai.application.trace.AiExecutionStatus;
import com.emme.assistant.ai.application.trace.AiModelExecutionTrace;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TracingAiChatCompletionTest {

  @Test
  void recordsSuccessfulModelExecutionWithBackendCorrelation() {
    AiChatCompletion delegate = mock(AiChatCompletion.class);
    when(delegate.complete(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new ChatResponse("Hi", "local-ollama", "gemma-v1", 0, 0));
    AiTraceRecorder recorder = mock(AiTraceRecorder.class);
    TracingAiChatCompletion port =
        new TracingAiChatCompletion(delegate, "local-ollama", "gemma-v1", "chat-v1", recorder);

    ChatResponse result =
        AiExecutionContextScope.call(
            context(), () -> port.complete(request("context", "hello ana@example.com")));

    assertThat(result).isEqualTo(new ChatResponse("Hi", "local-ollama", "gemma-v1", 0, 0));
    ArgumentCaptor<AiModelExecutionTrace> trace =
        ArgumentCaptor.forClass(AiModelExecutionTrace.class);
    verify(recorder).recordModelExecution(trace.capture());
    assertThat(trace.getValue().status()).isEqualTo(AiExecutionStatus.SUCCEEDED);
    assertThat(trace.getValue().providerKey()).isEqualTo("local-ollama");
    assertThat(trace.getValue().modelVersion()).isEqualTo("gemma-v1");
    assertThat(trace.getValue().promptVersion()).isEqualTo("chat-v1");
    assertThat(trace.getValue().inputTokens()).isNull();
    assertThat(trace.getValue().estimatedCost()).isNull();
    assertThat(trace.getValue().requestPayload()).contains("ana@example.com");
    assertThat(trace.getValue().latencyMillis()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void recordsProviderFailureAndRethrowsTheProviderFailure() {
    AiChatCompletion delegate = mock(AiChatCompletion.class);
    ChatProviderUnavailableException failure =
        new ChatProviderUnavailableException("connection refused");
    when(delegate.complete(org.mockito.ArgumentMatchers.any())).thenThrow(failure);
    AiTraceRecorder recorder = mock(AiTraceRecorder.class);
    TracingAiChatCompletion port =
        new TracingAiChatCompletion(delegate, "cloud", "cloud-v1", "chat-v1", recorder);

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    context(), () -> port.complete(request("context", "hello"))))
        .isSameAs(failure);

    ArgumentCaptor<AiModelExecutionTrace> trace =
        ArgumentCaptor.forClass(AiModelExecutionTrace.class);
    verify(recorder).recordModelExecution(trace.capture());
    assertThat(trace.getValue().status()).isEqualTo(AiExecutionStatus.FAILED);
    assertThat(trace.getValue().errorCode()).isEqualTo("ChatProviderUnavailableException");
  }

  @Test
  void doesNotMakeARequestFailWhenTracePersistenceFails() {
    AiChatCompletion delegate = mock(AiChatCompletion.class);
    when(delegate.complete(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new ChatResponse("Hi", "local", "model-v1", 0, 0));
    AiTraceRecorder recorder = mock(AiTraceRecorder.class);
    doThrow(new IllegalStateException("database down"))
        .when(recorder)
        .recordModelExecution(org.mockito.ArgumentMatchers.any());
    TracingAiChatCompletion port =
        new TracingAiChatCompletion(delegate, "local", "model-v1", "chat-v1", recorder);

    assertThat(
            AiExecutionContextScope.call(
                context(), () -> port.complete(request("context", "hello"))))
        .isEqualTo(new ChatResponse("Hi", "local", "model-v1", 0, 0));
  }

  private static AiChatCompletion.Request request(String conversationContext, String userMessage) {
    return new AiChatCompletion.Request(
        conversationContext,
        userMessage,
        context(),
        new AiChatCompletion.ProviderPolicy(List.of("local-ollama", "cloud", "local"), true));
  }

  private static AiExecutionContext context() {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        UUID.randomUUID(), UUID.randomUUID(), Set.of("ROLE_CLIENT"), id, id, "trace-1", "idem-1");
  }
}
