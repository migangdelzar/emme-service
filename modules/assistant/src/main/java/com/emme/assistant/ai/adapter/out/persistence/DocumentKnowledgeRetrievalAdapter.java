package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.KnowledgeDocument;
import com.emme.assistant.ai.application.port.out.KnowledgeRetrievalPort;
import com.emme.documents.api.query.SearchDocumentChunksQuery;
import com.emme.documents.api.usecase.SearchDocumentChunksUseCase;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Adapts the tenant-safe document use case to the application retrieval port. */
@Component
public final class DocumentKnowledgeRetrievalAdapter implements KnowledgeRetrievalPort {

  private final AiModelProvider legacyModel;
  private final SearchDocumentChunksUseCase searchDocuments;
  private final Optional<EmbeddingModelPort> embeddings;

  public DocumentKnowledgeRetrievalAdapter(
      AiModelProvider legacyModel,
      SearchDocumentChunksUseCase searchDocuments,
      Optional<EmbeddingModelPort> embeddings) {
    this.legacyModel = Objects.requireNonNull(legacyModel, "legacyModel must not be null");
    this.searchDocuments =
        Objects.requireNonNull(searchDocuments, "searchDocuments must not be null");
    this.embeddings = Objects.requireNonNull(embeddings, "embeddings must not be null");
  }

  @Override
  public List<KnowledgeDocument> retrieve(String question, int limit) {
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("question must not be blank");
    }
    if (limit <= 0) {
      throw new IllegalArgumentException("limit must be positive");
    }
    var context = AiExecutionContextScope.requireCurrent();
    List<Float> vector =
        embeddings
            .map(model -> model.embed(question).values())
            .orElseGet(() -> legacyModel.embed(question));
    return searchDocuments
        .search(new SearchDocumentChunksQuery(context.tenantId(), vector, question, limit))
        .stream()
        .filter(Objects::nonNull)
        .filter(chunk -> chunk.content() != null && !chunk.content().isBlank())
        .map(chunk -> new KnowledgeDocument(chunk.documentId().toString(), chunk.content(), 0.0))
        .toList();
  }
}
