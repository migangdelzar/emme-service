package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** PostgreSQL pgvector adapter for principal-scoped, expiring semantic-cache reads. */
@Component
public final class JdbcSemanticCacheAdapter implements SemanticCachePort {

  private static final int PGVECTOR_DIMENSIONS = 1024;

  private final JdbcClient jdbc;

  public JdbcSemanticCacheAdapter(JdbcClient jdbc) {
    this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
  }

  @Override
  public List<Candidate> find(Lookup lookup, int limit) {
    Objects.requireNonNull(lookup, "lookup must not be null");
    if (lookup.query().values().size() != PGVECTOR_DIMENSIONS) {
      throw new IllegalArgumentException("Embedding dimensions must match pgvector schema");
    }
    if (limit <= 0) {
      throw new IllegalArgumentException("limit must be greater than zero");
    }

    var context = AiExecutionContextScope.requireCurrent();
    return jdbc.sql(
            """
            SELECT id,
                   response_payload::text AS response_payload,
                   1 - (embedding <=> CAST(:queryEmbedding AS vector)) AS similarity
            FROM ai_semantic_cache
            WHERE tenant_id = :tenantId
              AND principal_id = :principalId
              AND cache_kind = :cacheKind
              AND context_fingerprint = :contextFingerprint
              AND prompt_version = :promptVersion
              AND embedding_model_version = :embeddingModelVersion
              AND active = true
              AND expires_at > CURRENT_TIMESTAMP
            ORDER BY embedding <=> CAST(:queryEmbedding AS vector), id
            LIMIT :limit
            """)
        .param("tenantId", context.tenantId())
        .param("principalId", context.principalId())
        .param("cacheKind", lookup.cacheKind())
        .param("contextFingerprint", lookup.contextFingerprint())
        .param("promptVersion", lookup.promptVersion())
        .param("embeddingModelVersion", lookup.query().modelVersion())
        .param("queryEmbedding", lookup.query().values().toString())
        .param("limit", limit)
        .query(
            (resultSet, rowNumber) ->
                new Candidate(
                    resultSet.getObject("id", java.util.UUID.class),
                    resultSet.getString("response_payload"),
                    resultSet.getDouble("similarity")))
        .list();
  }
}
