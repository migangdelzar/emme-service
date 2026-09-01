package com.emme.assistant.ai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.ai.contracts.rag.KnowledgeQuery;
import com.emme.ai.contracts.rag.RetrievedDocument;
import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.configuration.AiProperties;
import com.emme.documents.api.usecase.SearchDocumentChunksUseCase;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DocumentKnowledgeRetrievalAdapterTest {

  @Test
  void rejectsLegacyVectorsThatDoNotMatchTheConfiguredCatalogDimension() {
    AiModelProvider legacyModel = mock(AiModelProvider.class);
    when(legacyModel.embed("question")).thenReturn(Collections.nCopies(1024, 0.0f));
    DocumentKnowledgeRetrievalAdapter adapter =
        new DocumentKnowledgeRetrievalAdapter(
            legacyModel,
            mock(SearchDocumentChunksUseCase.class),
            Optional.<EmbeddingModelPort>empty(),
            new AiProperties(
                "mock",
                null,
                new AiProperties.EmbeddingConfig(
                    "embeddinggemma:300m", "http://localhost:11434", null, 768),
                true));

    AiExecutionContext context = context();
    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    context,
                    () -> adapter.search(new KnowledgeQuery("question", "es-MX", 1), context)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Embedding dimensions must match document_chunk schema");
  }

  @Test
  void searchesDocumentChunksForTheExplicitTenantContext() {
    AiModelProvider legacyModel = mock(AiModelProvider.class);
    when(legacyModel.embed("question")).thenReturn(Collections.nCopies(768, 0.0f));
    var searchDocuments = mock(SearchDocumentChunksUseCase.class);
    UUID tenantId = UUID.randomUUID();
    AiExecutionContext context = context(tenantId);
    UUID documentId = UUID.randomUUID();
    when(searchDocuments.search(any()))
        .thenReturn(
            List.of(
                new com.emme.documents.api.result.DocumentChunkDetails(
                    UUID.randomUUID(), documentId, 0, "The answer is here.", "fingerprint")));
    DocumentKnowledgeRetrievalAdapter adapter =
        new DocumentKnowledgeRetrievalAdapter(
            legacyModel,
            searchDocuments,
            Optional.<EmbeddingModelPort>empty(),
            new AiProperties(
                "mock",
                null,
                new AiProperties.EmbeddingConfig(
                    "embeddinggemma:300m", "http://localhost:11434", null, 768),
                true));

    List<RetrievedDocument> documents =
        AiExecutionContextScope.call(
            context, () -> adapter.search(new KnowledgeQuery("question", "es-MX", 1), context));

    assertThat(documents)
        .singleElement()
        .satisfies(
            document -> {
              assertThat(document.sourceId()).isEqualTo(documentId.toString());
              assertThat(document.content()).isEqualTo("The answer is here.");
            });
    ArgumentCaptor<com.emme.documents.api.query.SearchDocumentChunksQuery> query =
        ArgumentCaptor.forClass(com.emme.documents.api.query.SearchDocumentChunksQuery.class);
    org.mockito.Mockito.verify(searchDocuments).search(query.capture());
    assertThat(query.getValue().tenantId()).isEqualTo(tenantId);
    assertThat(query.getValue().queryText()).isEqualTo("question");
    assertThat(query.getValue().limit()).isEqualTo(1);
  }

  private static AiExecutionContext context() {
    return context(UUID.randomUUID());
  }

  private static AiExecutionContext context(UUID tenantId) {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        tenantId, UUID.randomUUID(), Set.of("client"), id, id, "trace-" + id, "idem-" + id);
  }
}
