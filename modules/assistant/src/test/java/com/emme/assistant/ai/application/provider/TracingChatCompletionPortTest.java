package com.emme.assistant.ai.application.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import com.emme.assistant.ai.application.trace.AiExecutionStatus;
import com.emme.assistant.ai.application.trace.AiModelExecutionTrace;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TracingChatCompletionPortTest {

  @Test
  void recordsSuccessfulModelExecutionWithBackendCorrelation() {
    ChatCompletionPort delegate = mock(ChatCompletionPort.class);
    when(delegate.complete("context", "hello ana@example.com")).thenReturn("Hi");
    AiTraceRecorder recorder = mock(AiTraceRecorder.class);
    TracingChatCompletionPort port =
        new TracingChatCompletionPort(delegate, "local-ollama", "gemma-v1", "chat-v1", recorder);

    String result =
        AiExecutionContextScope.call(
            context(), () -> port.complete("context", "hello ana@example.com"));

    assertThat(result).isEqualTo("Hi");
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
    ChatCompletionPort delegate = mock(ChatCompletionPort.class);
    ChatProviderUnavailableException failure =
        new ChatProviderUnavailableException("connection refused");
    when(delegate.complete("context", "hello")).thenThrow(failure);
    AiTraceRecorder recorder = mock(AiTraceRecorder.class);
    TracingChatCompletionPort port =
        new TracingChatCompletionPort(delegate, "cloud", "cloud-v1", "chat-v1", recorder);

    assertThatThrownBy(
            () -> AiExecutionContextScope.call(context(), () -> port.complete("context", "hello")))
        .isSameAs(failure);

    ArgumentCaptor<AiModelExecutionTrace> trace =
        ArgumentCaptor.forClass(AiModelExecutionTrace.class);
    verify(recorder).recordModelExecution(trace.capture());
    assertThat(trace.getValue().status()).isEqualTo(AiExecutionStatus.FAILED);
    assertThat(trace.getValue().errorCode()).isEqualTo("ChatProviderUnavailableException");
  }

  @Test
  void doesNotMakeARequestFailWhenTracePersistenceFails() {
    ChatCompletionPort delegate = mock(ChatCompletionPort.class);
    when(delegate.complete("context", "hello")).thenReturn("Hi");
    AiTraceRecorder recorder = mock(AiTraceRecorder.class);
    org.mockito.Mockito.doThrow(new IllegalStateException("database down"))
        .when(recorder)
        .recordModelExecution(org.mockito.ArgumentMatchers.any());
    TracingChatCompletionPort port =
        new TracingChatCompletionPort(delegate, "local", "model-v1", "chat-v1", recorder);

    assertThat(AiExecutionContextScope.call(context(), () -> port.complete("context", "hello")))
        .isEqualTo("Hi");
  }

  private static AiExecutionContext context() {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        UUID.randomUUID(), UUID.randomUUID(), Set.of("ROLE_CLIENT"), id, id, "trace-1", "idem-1");
  }
}
