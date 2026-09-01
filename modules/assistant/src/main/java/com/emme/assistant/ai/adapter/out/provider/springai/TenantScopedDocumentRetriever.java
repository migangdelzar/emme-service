package com.emme.assistant.ai.adapter.out.provider.springai;

import com.emme.assistant.ai.application.port.out.KnowledgeDocument;
import com.emme.assistant.ai.application.port.out.KnowledgeRetrievalPort;
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

  private final KnowledgeRetrievalPort retrieval;
  private final int limit;

  public TenantScopedDocumentRetriever(KnowledgeRetrievalPort retrieval, int limit) {
    this.retrieval = Objects.requireNonNull(retrieval, "retrieval must not be null");
    if (limit < 1 || limit > 20) {
      throw new IllegalArgumentException("limit must be between 1 and 20");
    }
    this.limit = limit;
  }

  @Override
  public List<Document> retrieve(Query query) {
    Objects.requireNonNull(query, "query must not be null");
    var queryText = query.text();
    if (queryText == null || queryText.isBlank()) {
      throw new IllegalArgumentException("query text must not be blank");
    }

    return toSpringDocuments(retrieval.retrieve(queryText, limit));
  }

  private List<Document> toSpringDocuments(List<KnowledgeDocument> chunks) {
    if (chunks == null || chunks.isEmpty()) {
      return List.of();
    }
    var documents = new ArrayList<Document>(chunks.size());
    for (var chunk : chunks) {
      if (chunk == null || chunk.content() == null || chunk.content().isBlank()) {
        continue;
      }
      var metadata = new java.util.LinkedHashMap<String, Object>();
      metadata.put("sourceId", chunk.sourceId());
      metadata.put("score", chunk.score());
      documents.add(new Document(chunk.sourceId(), chunk.content(), metadata));
    }
    return List.copyOf(documents);
  }
}
