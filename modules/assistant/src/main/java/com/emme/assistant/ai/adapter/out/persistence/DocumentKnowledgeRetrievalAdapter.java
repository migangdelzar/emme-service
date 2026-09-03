package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.ai.contracts.rag.KnowledgeQuery;
import com.emme.ai.contracts.rag.KnowledgeSearch;
import com.emme.ai.contracts.rag.RetrievedDocument;
import com.emme.ai.platform.configuration.AiProviderProperties;
import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.documents.api.query.SearchDocumentChunksQuery;
import com.emme.documents.api.usecase.SearchDocumentChunksUseCase;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Adapts the tenant-safe document use case to the application retrieval port. */
@Component
public final class DocumentKnowledgeRetrievalAdapter implements KnowledgeSearch {

  private final AiModelProvider legacyModel;
  private final SearchDocumentChunksUseCase searchDocuments;
  private final Optional<EmbeddingModelPort> embeddings;
  private final int embeddingDimensions;

  public DocumentKnowledgeRetrievalAdapter(
      AiModelProvider legacyModel,
      SearchDocumentChunksUseCase searchDocuments,
      Optional<EmbeddingModelPort> embeddings) {
    this(
        legacyModel,
        searchDocuments,
        embeddings,
        new AiProviderProperties(null, null, null, false));
  }

  public DocumentKnowledgeRetrievalAdapter(
      AiModelProvider legacyModel,
      SearchDocumentChunksUseCase searchDocuments,
      Optional<EmbeddingModelPort> embeddings,
      AiProviderProperties aiProperties) {
    this.legacyModel = Objects.requireNonNull(legacyModel, "legacyModel must not be null");
    this.searchDocuments =
        Objects.requireNonNull(searchDocuments, "searchDocuments must not be null");
    this.embeddings = Objects.requireNonNull(embeddings, "embeddings must not be null");
    this.embeddingDimensions =
        Objects.requireNonNull(aiProperties, "aiProperties must not be null").embeddingDimension();
  }

  @Override
  public List<RetrievedDocument> search(KnowledgeQuery query, AiExecutionContext context) {
    Objects.requireNonNull(query, "query must not be null");
    var boundContext = requireBoundContext(context);
    List<Float> vector =
        embeddings
            .map(model -> model.embed(query.text()).values())
            .orElseGet(() -> legacyModel.embed(query.text()));
    if (vector.size() != embeddingDimensions) {
      throw new IllegalArgumentException("Embedding dimensions must match document_chunk schema");
    }
    return searchDocuments
        .search(
            new SearchDocumentChunksQuery(
                boundContext.tenantId(), vector, query.text(), query.limit()))
        .stream()
        .filter(Objects::nonNull)
        .filter(chunk -> chunk.content() != null && !chunk.content().isBlank())
        .map(
            chunk ->
                new RetrievedDocument(
                    chunk.documentId().toString(), chunk.content(), java.util.Map.of(), 0.0))
        .toList();
  }

  private AiExecutionContext requireBoundContext(AiExecutionContext context) {
    var bound = AiExecutionContextScope.requireCurrent();
    if (!bound.equals(context)) {
      throw new IllegalArgumentException(
          "knowledge search context must match the bound backend context");
    }
    return bound;
  }
}
