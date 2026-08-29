package com.emme.assistant.ai.adapter.out.provider.springai;

import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.documents.api.query.SearchDocumentChunksQuery;
import com.emme.documents.api.result.DocumentChunkDetails;
import com.emme.documents.api.usecase.SearchDocumentChunksUseCase;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

/**
 * Adapts Emme's tenant-aware document search use case to Spring AI RAG retrieval.
 *
 * <p>The tenant is deliberately read from the backend AI execution scope. No tenant supplied by a
 * query, model, or retrieved document is accepted as authority.
 */
public final class TenantScopedDocumentRetriever implements DocumentRetriever {

  private final EmbeddingModelPort embeddings;
  private final SearchDocumentChunksUseCase searchDocuments;
  private final int limit;

  public TenantScopedDocumentRetriever(
      EmbeddingModelPort embeddings, SearchDocumentChunksUseCase searchDocuments, int limit) {
    this.embeddings = Objects.requireNonNull(embeddings, "embeddings must not be null");
    this.searchDocuments =
        Objects.requireNonNull(searchDocuments, "searchDocuments must not be null");
    if (limit < 1 || limit > 20) {
      throw new IllegalArgumentException("limit must be between 1 and 20");
    }
    this.limit = limit;
  }

  @Override
  public List<Document> retrieve(Query query) {
    Objects.requireNonNull(query, "query must not be null");
    var executionContext = AiExecutionContextScope.requireCurrent();
    var queryText = query.text();
    if (queryText == null || queryText.isBlank()) {
      throw new IllegalArgumentException("query text must not be blank");
    }

    var vector = embeddings.embed(queryText);
    var chunks =
        searchDocuments.search(
            new SearchDocumentChunksQuery(
                executionContext.tenantId(), vector.values(), queryText, limit));
    return toSpringDocuments(chunks, executionContext.tenantId().toString());
  }

  private List<Document> toSpringDocuments(List<DocumentChunkDetails> chunks, String tenantId) {
    if (chunks == null || chunks.isEmpty()) {
      return List.of();
    }
    var documents = new ArrayList<Document>(chunks.size());
    for (var chunk : chunks) {
      if (chunk == null || chunk.content() == null || chunk.content().isBlank()) {
        continue;
      }
      var metadata = new java.util.LinkedHashMap<String, Object>();
      metadata.put("tenantId", tenantId);
      metadata.put("sourceId", chunk.documentId().toString());
      metadata.put("chunkId", chunk.id().toString());
      metadata.put("chunkIndex", chunk.chunkIndex());
      if (chunk.contentFingerprint() != null && !chunk.contentFingerprint().isBlank()) {
        metadata.put("contentFingerprint", chunk.contentFingerprint());
      }
      documents.add(new Document(chunk.id().toString(), chunk.content(), metadata));
    }
    return List.copyOf(documents);
  }
}
