package com.emme.assistant.ai.adapter.out.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.KnowledgeDocument;
import com.emme.assistant.ai.application.port.out.KnowledgeRetrievalPort;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.rag.Query;

class TenantScopedDocumentRetrieverTest {

  @Test
  void adaptsFrameworkNeutralResultsToSpringDocuments() {
    KnowledgeRetrievalPort retrieval = mock(KnowledgeRetrievalPort.class);
    when(retrieval.retrieve("What is the cancellation policy?", 5))
        .thenReturn(
            List.of(new KnowledgeDocument("source-1", "Cancellation requires 24 hours.", 0.92)));
    TenantScopedDocumentRetriever retriever = new TenantScopedDocumentRetriever(retrieval, 5);

    List<org.springframework.ai.document.Document> documents =
        retriever.retrieve(new Query("What is the cancellation policy?"));

    assertThat(documents).hasSize(1);
    assertThat(documents.getFirst().getText()).isEqualTo("Cancellation requires 24 hours.");
    assertThat(documents.getFirst().getMetadata())
        .containsEntry("sourceId", "source-1")
        .containsEntry("score", 0.92);
    verify(retrieval).retrieve("What is the cancellation policy?", 5);
  }

  @Test
  void failsClosedWhenTheQueryIsBlank() {
    KnowledgeRetrievalPort retrieval = mock(KnowledgeRetrievalPort.class);
    TenantScopedDocumentRetriever retriever = new TenantScopedDocumentRetriever(retrieval, 5);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> retriever.retrieve(new Query(" ")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("text cannot be null or empty");
    verifyNoInteractions(retrieval);
  }
}
