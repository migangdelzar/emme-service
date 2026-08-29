package com.emme.assistant.ai.adapter.out.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.semantic.EmbeddingVector;
import com.emme.documents.api.result.DocumentChunkDetails;
import com.emme.documents.api.usecase.SearchDocumentChunksUseCase;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.rag.Query;

class TenantScopedDocumentRetrieverTest {

  @Test
  void embedsAndSearchesUsingTheBackendTenantContext() {
    UUID tenantId = UUID.randomUUID();
    EmbeddingModelPort embeddings = mock(EmbeddingModelPort.class);
    SearchDocumentChunksUseCase search = mock(SearchDocumentChunksUseCase.class);
    UUID chunkId = UUID.randomUUID();
    UUID documentId = UUID.randomUUID();
    when(embeddings.embed("What is the cancellation policy?"))
        .thenReturn(new EmbeddingVector("embedding-v1", List.of(0.1f, 0.2f)));
    when(search.search(any()))
        .thenReturn(
            List.of(
                new DocumentChunkDetails(
                    chunkId, documentId, 2, "Cancellation requires 24 hours.", "fingerprint")));
    TenantScopedDocumentRetriever retriever =
        new TenantScopedDocumentRetriever(embeddings, search, 5);

    List<org.springframework.ai.document.Document> documents =
        AiExecutionContextScope.call(
            context(tenantId),
            () -> retriever.retrieve(new Query("What is the cancellation policy?")));

    assertThat(documents).hasSize(1);
    assertThat(documents.getFirst().getText()).isEqualTo("Cancellation requires 24 hours.");
    assertThat(documents.getFirst().getMetadata())
        .containsEntry("tenantId", tenantId.toString())
        .containsEntry("sourceId", documentId.toString())
        .containsEntry("chunkId", chunkId.toString())
        .containsEntry("chunkIndex", 2);
    var captured =
        org.mockito.ArgumentCaptor.forClass(
            com.emme.documents.api.query.SearchDocumentChunksQuery.class);
    verify(search).search(captured.capture());
    assertThat(captured.getValue().tenantId()).isEqualTo(tenantId);
    assertThat(captured.getValue().queryText()).isEqualTo("What is the cancellation policy?");
  }

  @Test
  void failsClosedWhenTheBackendAiContextIsMissing() {
    EmbeddingModelPort embeddings = mock(EmbeddingModelPort.class);
    SearchDocumentChunksUseCase search = mock(SearchDocumentChunksUseCase.class);
    TenantScopedDocumentRetriever retriever =
        new TenantScopedDocumentRetriever(embeddings, search, 5);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> retriever.retrieve(new Query("hello")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
    verifyNoInteractions(embeddings, search);
  }

  private static AiExecutionContext context(UUID tenantId) {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        tenantId, UUID.randomUUID(), Set.of("client"), id, id, "trace-" + id, "idem-" + id);
  }
}
