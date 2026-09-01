package com.emme.assistant.ai.adapter.out.provider.springai;

import com.emme.ai.contracts.semantic.EmbeddingModelConfiguration;
import com.emme.ai.contracts.semantic.EmbeddingModelDefaults;
import com.emme.assistant.ai.application.port.out.SemanticCacheHotStore;
import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.semantic.EmbeddingVector;
import com.emme.assistant.ai.application.semantic.SemanticCacheInvalidation;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import redis.clients.jedis.RedisClient;

/**
 * Spring AI Redis vector-store projection for the durable semantic cache.
 *
 * <p>Redis is deliberately only a hot projection. The returned id is the PostgreSQL cache id so the
 * application can still atomically confirm the hit against the durable, tenant-scoped row.
 */
public final class RedisSemanticCacheHotStore implements SemanticCacheHotStore {

  private static final String DOCUMENT_ID_PREFIX = "cache-";

  private final VectorStore vectorStore;
  private final String embeddingModelName;
  private final String embeddingModelVersion;
  private final int embeddingDimensions;
  private final Clock clock;
  private final RedisClient redisClient;
  private final String redisKeyPrefix;

  public RedisSemanticCacheHotStore(
      VectorStore vectorStore, String embeddingModelVersion, int embeddingDimensions) {
    this(
        vectorStore,
        EmbeddingModelDefaults.MODEL_NAME,
        embeddingModelVersion,
        embeddingDimensions,
        Clock.systemUTC());
  }

  public RedisSemanticCacheHotStore(
      VectorStore vectorStore, String embeddingModelVersion, int embeddingDimensions, Clock clock) {
    this(
        vectorStore,
        EmbeddingModelDefaults.MODEL_NAME,
        embeddingModelVersion,
        embeddingDimensions,
        clock,
        null,
        "");
  }

  public RedisSemanticCacheHotStore(
      VectorStore vectorStore,
      String embeddingModelName,
      String embeddingModelVersion,
      int embeddingDimensions,
      Clock clock) {
    this(
        vectorStore,
        embeddingModelName,
        embeddingModelVersion,
        embeddingDimensions,
        clock,
        null,
        "");
  }

  public RedisSemanticCacheHotStore(
      VectorStore vectorStore,
      String embeddingModelVersion,
      int embeddingDimensions,
      Clock clock,
      RedisClient redisClient,
      String redisKeyPrefix) {
    this(
        vectorStore,
        EmbeddingModelDefaults.MODEL_NAME,
        embeddingModelVersion,
        embeddingDimensions,
        clock,
        redisClient,
        redisKeyPrefix);
  }

  public RedisSemanticCacheHotStore(
      VectorStore vectorStore,
      String embeddingModelName,
      String embeddingModelVersion,
      int embeddingDimensions,
      Clock clock,
      RedisClient redisClient,
      String redisKeyPrefix) {
    this.vectorStore = Objects.requireNonNull(vectorStore, "vectorStore must not be null");
    if (embeddingModelName == null || embeddingModelName.isBlank()) {
      throw new IllegalArgumentException("embeddingModelName must not be blank");
    }
    if (embeddingModelVersion == null || embeddingModelVersion.isBlank()) {
      throw new IllegalArgumentException("embeddingModelVersion must not be blank");
    }
    if (embeddingDimensions <= 0) {
      throw new IllegalArgumentException("embeddingDimensions must be positive");
    }
    this.embeddingModelName = embeddingModelName;
    this.embeddingModelVersion = embeddingModelVersion;
    this.embeddingDimensions = embeddingDimensions;
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
    this.redisClient = redisClient;
    if (redisKeyPrefix == null) {
      throw new NullPointerException("redisKeyPrefix must not be null");
    }
    this.redisKeyPrefix = redisKeyPrefix;
  }

  public RedisSemanticCacheHotStore(
      VectorStore vectorStore,
      EmbeddingModelConfiguration embeddingConfiguration,
      Clock clock,
      RedisClient redisClient,
      String redisKeyPrefix) {
    this(
        vectorStore,
        embeddingConfiguration.modelName(),
        embeddingConfiguration.modelVersion(),
        embeddingConfiguration.dimension(),
        clock,
        redisClient,
        redisKeyPrefix);
  }

  @Override
  public List<SemanticCachePort.Candidate> find(
      SemanticCachePort.Lookup lookup, String queryText, int limit) {
    Objects.requireNonNull(lookup, "lookup must not be null");
    requireText(queryText, "queryText");
    requireLimit(limit);
    validateEmbedding(lookup.query());
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();

    FilterExpressionBuilder filters = new FilterExpressionBuilder();
    var tenantAndPrincipal =
        filters.and(
            filters.eq("tenantId", encodeTagValue(context.tenantId().toString())),
            filters.eq("principalId", encodeTagValue(context.principalId().toString())));
    var kindAndContext =
        filters.and(
            filters.eq("cacheKind", encodeTagValue(lookup.cacheKind())),
            filters.eq("contextFingerprint", encodeTagValue(lookup.contextFingerprint())));
    var versions =
        filters.and(
            filters.eq("promptVersion", encodeTagValue(lookup.promptVersion())),
            filters.and(
                filters.eq("embeddingModelName", encodeTagValue(embeddingModelName)),
                filters.eq(
                    "embeddingModelVersion", encodeTagValue(lookup.query().modelVersion()))));
    var responseIdentity =
        filters.eq("responseProvider", encodeTagValue(lookup.identity().responseProvider()));
    responseIdentity =
        filters.and(
            responseIdentity,
            filters.eq("responseModel", encodeTagValue(lookup.identity().responseModel())));
    responseIdentity =
        filters.and(
            responseIdentity,
            filters.eq("knowledgeVersion", encodeTagValue(lookup.identity().knowledgeVersion())));
    responseIdentity =
        filters.and(
            responseIdentity,
            filters.eq("policyVersion", encodeTagValue(lookup.identity().policyVersion())));
    responseIdentity =
        filters.and(
            responseIdentity,
            filters.eq("sourceVersion", encodeTagValue(lookup.identity().sourceVersion())));
    responseIdentity =
        filters.and(
            responseIdentity,
            filters.eq("responseChannel", encodeTagValue(lookup.identity().channel())));
    responseIdentity =
        filters.and(
            responseIdentity,
            filters.eq("responseLocale", encodeTagValue(lookup.identity().locale())));
    responseIdentity =
        filters.and(
            responseIdentity,
            filters.eq(
                "responseQuoteTemplateVersion",
                encodeTagValue(lookup.identity().quoteTemplateVersion())));
    var identity =
        filters.and(
            filters.and(tenantAndPrincipal, kindAndContext),
            filters.and(versions, responseIdentity));
    var filter =
        filters.and(identity, filters.gt("expiresAt", Instant.now(clock).getEpochSecond())).build();

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
        Map.ofEntries(
            Map.entry("tenantId", encodeTagValue(context.tenantId().toString())),
            Map.entry("principalId", encodeTagValue(context.principalId().toString())),
            Map.entry("durableCacheId", durableCacheId.toString()),
            Map.entry("cacheKind", encodeTagValue(write.cacheKind())),
            Map.entry("contextFingerprint", encodeTagValue(write.contextFingerprint())),
            Map.entry("promptVersion", encodeTagValue(write.promptVersion())),
            Map.entry("embeddingModelName", encodeTagValue(embeddingModelName)),
            Map.entry("embeddingModelVersion", encodeTagValue(write.query().modelVersion())),
            Map.entry("responseProvider", encodeTagValue(write.identity().responseProvider())),
            Map.entry("responseModel", encodeTagValue(write.identity().responseModel())),
            Map.entry("knowledgeVersion", encodeTagValue(write.identity().knowledgeVersion())),
            Map.entry("policyVersion", encodeTagValue(write.identity().policyVersion())),
            Map.entry("sourceVersion", encodeTagValue(write.identity().sourceVersion())),
            Map.entry("responseChannel", encodeTagValue(write.identity().channel())),
            Map.entry("responseLocale", encodeTagValue(write.identity().locale())),
            Map.entry(
                "responseQuoteTemplateVersion",
                encodeTagValue(write.identity().quoteTemplateVersion())),
            Map.entry("responsePayload", write.responsePayload()),
            Map.entry("expiresAt", write.expiresAt().getEpochSecond()));
    vectorStore.add(
        List.of(
            Document.builder()
                .id(DOCUMENT_ID_PREFIX + durableCacheId)
                .text(write.queryText())
                .metadata(metadata)
                .build()));
    indexProjection(context, durableCacheId, write.expiresAt());
    expireProjection(durableCacheId, write.expiresAt());
  }

  @Override
  public void invalidate(SemanticCacheInvalidation invalidation) {
    Objects.requireNonNull(invalidation, "invalidation must not be null");
    if (redisClient == null) {
      return;
    }
    String tenantIndex = tenantIndexKey(invalidation.tenantId());
    if (invalidation.principalId() != null) {
      deletePrincipalProjection(
          principalIndexKey(invalidation.tenantId(), invalidation.principalId()));
      return;
    }
    for (String principalIndex : redisClient.smembers(tenantIndex)) {
      deletePrincipalProjection(principalIndex);
    }
    redisClient.del(tenantIndex);
  }

  private void indexProjection(AiExecutionContext context, UUID durableCacheId, Instant expiresAt) {
    if (redisClient == null) {
      return;
    }
    String tenantIndex = tenantIndexKey(context.tenantId());
    String principalIndex = principalIndexKey(context.tenantId(), context.principalId());
    String documentKey = redisKeyPrefix + DOCUMENT_ID_PREFIX + durableCacheId;
    redisClient.sadd(principalIndex, documentKey);
    redisClient.sadd(tenantIndex, principalIndex);
    long ttlSeconds = Math.max(1L, Duration.between(Instant.now(clock), expiresAt).getSeconds());
    redisClient.expire(principalIndex, ttlSeconds);
    redisClient.expire(tenantIndex, ttlSeconds);
  }

  private void deletePrincipalProjection(String principalIndex) {
    for (String documentKey : redisClient.smembers(principalIndex)) {
      redisClient.del(documentKey);
    }
    redisClient.del(principalIndex);
  }

  private String tenantIndexKey(UUID tenantId) {
    return redisKeyPrefix + "tenant:" + tenantId;
  }

  private String principalIndexKey(UUID tenantId, UUID principalId) {
    return tenantIndexKey(tenantId) + ":principal:" + principalId;
  }

  private void expireProjection(UUID durableCacheId, Instant expiresAt) {
    if (redisClient == null) {
      return;
    }
    long ttlSeconds = Math.max(1L, Duration.between(Instant.now(clock), expiresAt).getSeconds());
    redisClient.expire(redisKeyPrefix + DOCUMENT_ID_PREFIX + durableCacheId, ttlSeconds);
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

  private static String encodeTagValue(String value) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
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
