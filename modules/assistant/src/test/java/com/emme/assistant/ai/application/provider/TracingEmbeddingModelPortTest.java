package com.emme.assistant.ai.application.provider;

import static com.emme.assistant.ai.EmbeddingTestVectors.testEmbedding;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.embedding.EmbeddingService;
import com.emme.ai.contracts.semantic.EmbeddingVector;
import com.emme.assistant.ai.application.port.out.EmbeddingProviderUnavailableException;
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

class TracingEmbeddingModelPortTest {

  @Test
  void recordsSuccessfulEmbeddingAttemptsWithoutPersistingVectorValues() {
    EmbeddingService delegate = mock(EmbeddingService.class);
    when(delegate.embed("faq ana@example.com"))
        .thenReturn(testEmbedding("bge-v1", List.of(0.2f, 0.8f)));
    AiTraceRecorder recorder = mock(AiTraceRecorder.class);
    TracingEmbeddingModelPort port =
        new TracingEmbeddingModelPort(delegate, "local-ollama", "bge-v1", "embedding-v1", recorder);

    EmbeddingVector result =
        AiExecutionContextScope.call(context(), () -> port.embed("faq ana@example.com"));

    assertThat(result.values()).containsExactly(0.2f, 0.8f);
    ArgumentCaptor<AiModelExecutionTrace> trace =
        ArgumentCaptor.forClass(AiModelExecutionTrace.class);
    verify(recorder).recordModelExecution(trace.capture());
    assertThat(trace.getValue().status()).isEqualTo(AiExecutionStatus.SUCCEEDED);
    assertThat(trace.getValue().operation()).isEqualTo("EMBEDDING");
    assertThat(trace.getValue().responsePayload()).isEqualTo("dimension=2");
    assertThat(trace.getValue().responsePayload()).doesNotContain("0.2");
  }

  @Test
  void recordsEmbeddingProviderFailureAndPreservesFailoverSemantics() {
    EmbeddingService delegate = mock(EmbeddingService.class);
    EmbeddingProviderUnavailableException failure =
        new EmbeddingProviderUnavailableException("embedding timeout");
    when(delegate.embed("faq")).thenThrow(failure);
    AiTraceRecorder recorder = mock(AiTraceRecorder.class);
    TracingEmbeddingModelPort port =
        new TracingEmbeddingModelPort(delegate, "local-ollama", "bge-v1", "embedding-v1", recorder);

    assertThatThrownBy(() -> AiExecutionContextScope.call(context(), () -> port.embed("faq")))
        .isSameAs(failure);

    ArgumentCaptor<AiModelExecutionTrace> trace =
        ArgumentCaptor.forClass(AiModelExecutionTrace.class);
    verify(recorder).recordModelExecution(trace.capture());
    assertThat(trace.getValue().status()).isEqualTo(AiExecutionStatus.FAILED);
    assertThat(trace.getValue().errorCode()).isEqualTo("EmbeddingProviderUnavailableException");
  }

  private static AiExecutionContext context() {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        UUID.randomUUID(), UUID.randomUUID(), Set.of("ROLE_CLIENT"), id, id, "trace-1", "idem-1");
  }
}
