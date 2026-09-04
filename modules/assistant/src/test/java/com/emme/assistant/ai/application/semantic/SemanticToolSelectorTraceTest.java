package com.emme.assistant.ai.application.semantic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.SemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticReferenceSearchPort;
import com.emme.assistant.ai.application.trace.AiSemanticExecutionTrace;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SemanticToolSelectorTraceTest {

  @Test
  void recordsThatSelectionWasAbstainedWhenNoToolsAreAuthorized() {
    AiTraceRecorder traces = mock(AiTraceRecorder.class);
    SemanticToolSelector selector =
        new SemanticToolSelector(
            mock(SemanticReferenceSearchPort.class),
            new SemanticMatchPolicy(0.9, 0.1),
            mock(SemanticMetrics.class),
            traces);

    assertThat(selector.select("es-MX", new EmbeddingVector("v1", List.of(1.0f)), Set.of()))
        .isEqualTo(new SemanticDecision(java.util.Optional.empty(), 0.0, 0.0, 0.0, false));

    ArgumentCaptor<AiSemanticExecutionTrace> trace =
        ArgumentCaptor.forClass(AiSemanticExecutionTrace.class);
    org.mockito.Mockito.verify(traces).recordSemanticOutcome(trace.capture());
    assertThat(trace.getValue().outcome()).isEqualTo("no_authorized_tools");
  }

  @Test
  void recordsSemanticSearchFailuresBeforeRethrowingThem() {
    SemanticReferenceSearchPort search = mock(SemanticReferenceSearchPort.class);
    RuntimeException failure = new IllegalStateException("vector store unavailable");
    when(search.searchTools(
            "es-MX", new EmbeddingVector("v1", List.of(1.0f)), Set.of("getServices"), 2))
        .thenThrow(failure);
    AiTraceRecorder traces = mock(AiTraceRecorder.class);
    SemanticToolSelector selector =
        new SemanticToolSelector(
            search, new SemanticMatchPolicy(0.9, 0.1), mock(SemanticMetrics.class), traces);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                selector.select(
                    "es-MX", new EmbeddingVector("v1", List.of(1.0f)), Set.of("getServices")))
        .isSameAs(failure);

    ArgumentCaptor<AiSemanticExecutionTrace> trace =
        ArgumentCaptor.forClass(AiSemanticExecutionTrace.class);
    org.mockito.Mockito.verify(traces).recordSemanticOutcome(trace.capture());
    assertThat(trace.getValue().outcome()).isEqualTo("failed");
  }
}
