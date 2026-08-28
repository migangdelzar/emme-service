package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.SemanticReferenceSearchPort;
import com.emme.assistant.ai.application.semantic.EmbeddingVector;
import com.emme.assistant.ai.application.semantic.SemanticIntentClassifier;
import com.emme.assistant.ai.application.semantic.SemanticIntentRouter;
import com.emme.assistant.ai.application.semantic.SemanticMatch;
import com.emme.assistant.ai.application.semantic.SemanticMatchPolicy;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SemanticIntentRouterTest {

  private static final EmbeddingVector QUERY =
      new EmbeddingVector("embedding-v1", List.of(1.0f, 0.0f));

  @Test
  void returnsAValidatedSemanticIntentWithoutCallingAnLlm() {
    EmbeddingModelPort embeddings = mock(EmbeddingModelPort.class);
    SemanticReferenceSearchPort search = mock(SemanticReferenceSearchPort.class);
    when(embeddings.embed("book Friday afternoon")).thenReturn(QUERY);
    when(search.searchIntents("es-MX", QUERY, 2))
        .thenReturn(List.of(new SemanticMatch("BOOK_APPOINTMENT", 0.97)));
    SemanticIntentRouter router =
        new SemanticIntentRouter(
            embeddings,
            new SemanticIntentClassifier(search, new SemanticMatchPolicy(0.90, 0.10)),
            "es-MX");

    Optional<com.emme.assistant.ai.api.result.IntentResult> result =
        router.route("book Friday afternoon");

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().intent()).isEqualTo("BOOK_APPOINTMENT");
    assertThat(result.orElseThrow().confidence()).isEqualTo(0.97);
    assertThat(result.orElseThrow().parameters()).containsEntry("routing", "semantic");
    verify(embeddings).embed("book Friday afternoon");
  }

  @Test
  void abstainsWhenTheSemanticGateRejectsTheCandidates() {
    EmbeddingModelPort embeddings = mock(EmbeddingModelPort.class);
    SemanticReferenceSearchPort search = mock(SemanticReferenceSearchPort.class);
    when(embeddings.embed("maybe Friday")).thenReturn(QUERY);
    when(search.searchIntents("es-MX", QUERY, 2))
        .thenReturn(
            List.of(
                new SemanticMatch("BOOK_APPOINTMENT", 0.91),
                new SemanticMatch("CHECK_AVAILABILITY", 0.89)));
    SemanticIntentRouter router =
        new SemanticIntentRouter(
            embeddings,
            new SemanticIntentClassifier(search, new SemanticMatchPolicy(0.90, 0.10)),
            "es-MX");

    assertThat(router.route("maybe Friday")).isEmpty();
  }

  @Test
  void rejectsBlankInputBeforeEmbedding() {
    EmbeddingModelPort embeddings = mock(EmbeddingModelPort.class);
    SemanticIntentRouter router =
        new SemanticIntentRouter(embeddings, mock(SemanticIntentClassifier.class), "es-MX");

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> router.route(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("message must not be blank");
    verifyNoInteractions(embeddings);
  }
}
