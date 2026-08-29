package com.emme.assistant.ai.adapter.out.provider.springai;

import com.emme.assistant.ai.application.port.out.SemanticCacheHotStore;
import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.semantic.EmbeddingVector;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * Spring AI Redis vector-store projection for the durable semantic cache.
 *
 * <p>Redis is deliberately only a hot projection. The returned id is the PostgreSQL cache id so the
 * application can still atomically confirm the hit against the durable, tenant-scoped row.
 */
public final class RedisSemanticCacheHotStore implements SemanticCacheHotStore {

  private static final String DOCUMENT_ID_PREFIX = "cache-";

  private final VectorStore vectorStore;
  private final String embeddingModelVersion;
  private final int embeddingDimensions;
  private final Clock clock;

  public RedisSemanticCacheHotStore(
      VectorStore vectorStore, String embeddingModelVersion, int embeddingDimensions) {
    this(vectorStore, embeddingModelVersion, embeddingDimensions, Clock.systemUTC());
  }

  public RedisSemanticCacheHotStore(
      VectorStore vectorStore, String embeddingModelVersion, int embeddingDimensions, Clock clock) {
    this.vectorStore = Objects.requireNonNull(vectorStore, "vectorStore must not be null");
    if (embeddingModelVersion == null || embeddingModelVersion.isBlank()) {
      throw new IllegalArgumentException("embeddingModelVersion must not be blank");
    }
    if (embeddingDimensions <= 0) {
      throw new IllegalArgumentException("embeddingDimensions must be positive");
    }
    this.embeddingModelVersion = embeddingModelVersion;
    this.embeddingDimensions = embeddingDimensions;
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  @Override
  public List<SemanticCachePort.Candidate> find(
      SemanticCachePort.Lookup lookup, String queryText, int limit) {
    Objects.requireNonNull(lookup, "lookup must not be null");
    requireText(queryText, "queryText");
    requireLimit(limit);
    validateEmbedding(lookup.query());
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();

    String filter =
        "tenantId == '"
            + context.tenantId()
            + "' && principalId == '"
            + context.principalId()
            + "' && cacheKind == '"
            + escapeFilterValue(lookup.cacheKind())
            + "' && contextFingerprint == '"
            + escapeFilterValue(lookup.contextFingerprint())
            + "' && promptVersion == '"
            + escapeFilterValue(lookup.promptVersion())
            + "' && embeddingModelVersion == '"
            + escapeFilterValue(lookup.query().modelVersion())
            + "' && expiresAt > "
            + Instant.now(clock).getEpochSecond();

    return vectorStore
        .similaritySearch(
            SearchRequest.builder()
                .query(queryText)
                .topK(limit)
                .similarityThresholdAll()
                .filterExpression(filter)
                .build())
        .stream()
        .map(RedisSemanticCacheHotStore::candidate)
        .filter(Objects::nonNull)
        .filter(candidate -> candidate.similarity() >= 0.0 && candidate.similarity() <= 1.0)
        .toList();
  }

  @Override
  public void put(UUID durableCacheId, SemanticCachePort.Put write) {
    Objects.requireNonNull(durableCacheId, "durableCacheId must not be null");
    Objects.requireNonNull(write, "write must not be null");
    validateEmbedding(write.query());
    if (!write.expiresAt().isAfter(Instant.now(clock))) {
      throw new IllegalArgumentException("expiresAt must be in the future");
    }
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();

    Map<String, Object> metadata =
        Map.of(
            "tenantId", context.tenantId().toString(),
            "principalId", context.principalId().toString(),
            "durableCacheId", durableCacheId.toString(),
            "cacheKind", write.cacheKind(),
            "contextFingerprint", write.contextFingerprint(),
            "promptVersion", write.promptVersion(),
            "embeddingModelVersion", write.query().modelVersion(),
            "responsePayload", write.responsePayload(),
            "expiresAt", write.expiresAt().getEpochSecond());
    vectorStore.add(
        List.of(
            Document.builder()
                .id(DOCUMENT_ID_PREFIX + durableCacheId)
                .text(write.queryText())
                .metadata(metadata)
                .build()));
  }

  private void validateEmbedding(EmbeddingVector embedding) {
    Objects.requireNonNull(embedding, "embedding must not be null");
    if (embedding.values().size() != embeddingDimensions) {
      throw new IllegalArgumentException("Embedding dimensions must match Redis vector schema");
    }
    if (!embeddingModelVersion.equals(embedding.modelVersion())) {
      throw new IllegalArgumentException("Embedding model version must match Redis vector schema");
    }
  }

  private static SemanticCachePort.Candidate candidate(Document document) {
    try {
      UUID durableId =
          UUID.fromString(String.valueOf(document.getMetadata().get("durableCacheId")));
      String responsePayload = String.valueOf(document.getMetadata().get("responsePayload"));
      Double score = document.getScore();
      return new SemanticCachePort.Candidate(
          durableId, responsePayload, score == null ? 0.0 : score);
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private static String escapeFilterValue(String value) {
    return value.replace("\\", "\\\\").replace("'", "\\'");
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }

  private static void requireLimit(int limit) {
    if (limit <= 0) {
      throw new IllegalArgumentException("limit must be greater than zero");
    }
  }
}
