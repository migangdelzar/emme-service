package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.kernel.context.AiExecutionContextScope;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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

  @Override
  public UUID put(Put write) {
    Objects.requireNonNull(write, "write must not be null");
    if (write.query().values().size() != PGVECTOR_DIMENSIONS) {
      throw new IllegalArgumentException("Embedding dimensions must match pgvector schema");
    }
    if (!write.expiresAt().isAfter(Instant.now())) {
      throw new IllegalArgumentException("expiresAt must be in the future");
    }

    var context = AiExecutionContextScope.requireCurrent();
    return jdbc.sql(
            """
            INSERT INTO ai_semantic_cache (
                tenant_id,
                principal_id,
                cache_kind,
                query_text,
                context_fingerprint,
                embedding,
                embedding_model_version,
                prompt_version,
                response_payload,
                expires_at,
                write_idempotency_key
            )
            VALUES (
                :tenantId,
                :principalId,
                :cacheKind,
                :queryText,
                :contextFingerprint,
                CAST(:queryEmbedding AS vector),
                :embeddingModelVersion,
                :promptVersion,
                CAST(:responsePayload AS jsonb),
                :expiresAt,
                :writeIdempotencyKey
            )
            ON CONFLICT (tenant_id, principal_id, write_idempotency_key)
            DO UPDATE SET updated_at = ai_semantic_cache.updated_at
            RETURNING id
            """)
        .param("tenantId", context.tenantId())
        .param("principalId", context.principalId())
        .param("cacheKind", write.cacheKind())
        .param("queryText", write.queryText())
        .param("contextFingerprint", write.contextFingerprint())
        .param("queryEmbedding", write.query().values().toString())
        .param("embeddingModelVersion", write.query().modelVersion())
        .param("promptVersion", write.promptVersion())
        .param("responsePayload", write.responsePayload())
        .param("expiresAt", Timestamp.from(write.expiresAt()))
        .param("writeIdempotencyKey", write.writeIdempotencyKey())
        .query((resultSet, rowNumber) -> resultSet.getObject("id", UUID.class))
        .single();
  }

  @Override
  public boolean recordHit(UUID cacheId) {
    Objects.requireNonNull(cacheId, "cacheId must not be null");
    var context = AiExecutionContextScope.requireCurrent();
    return jdbc.sql(
                """
                UPDATE ai_semantic_cache
                SET hit_count = hit_count + 1,
                    last_hit_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = :cacheId
                  AND tenant_id = :tenantId
                  AND principal_id = :principalId
                  AND active = true
                  AND expires_at > CURRENT_TIMESTAMP
                """)
            .param("cacheId", cacheId)
            .param("tenantId", context.tenantId())
            .param("principalId", context.principalId())
            .update()
        > 0;
  }
}
