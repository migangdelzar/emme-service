package com.emme.assistant.ai.application.semantic;

import com.emme.ai.contracts.embedding.EmbeddingService;
import com.emme.ai.contracts.semantic.EmbeddingModelConfiguration;
import com.emme.ai.contracts.semantic.EmbeddingVector;
import com.emme.assistant.ai.application.port.out.EmbeddingProviderUnavailableException;
import com.emme.assistant.ai.application.port.out.SemanticCacheHotStore;
import com.emme.assistant.ai.application.port.out.SemanticCachePayloadCodec;
import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.port.out.SemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticResponseCache;
import com.emme.assistant.ai.application.trace.AiSemanticExecutionTrace;
import com.emme.assistant.ai.application.trace.AiTracePersistenceFailureReporter;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.kernel.context.AiExecutionContextScope;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Principal-scoped semantic cache for safe informational chat responses.
 *
 * <p>Transactional requests never enter this cache. PostgreSQL remains authoritative for the cached
 * response and hit accounting; this service only defines eligibility and cache identity.
 */
public final class SemanticChatCache implements SemanticResponseCache {

  private static final Logger LOGGER = LoggerFactory.getLogger(SemanticChatCache.class);

  private static final String CACHE_KIND = "CHAT_INFORMATIONAL";

  private final EmbeddingService legacyEmbeddings;
  private final SemanticCacheResolver resolver;
  private final SemanticCachePort cache;
  private final SemanticCachePayloadCodec codec;
  private final Clock clock;
  private final String promptVersion;
  private final Duration ttl;
  private final Optional<SemanticCacheHotStore> hotStore;
  private final SemanticMetrics metrics;
  private final EmbeddingModelConfiguration embeddingModelConfiguration;
  private final SemanticCacheIdentity identity;
  private final String locale;
  private final String quoteTemplateVersion;
  private final AiTraceRecorder traceRecorder;

  public SemanticChatCache(
      EmbeddingService embeddings,
      SemanticCacheResolver resolver,
      SemanticCachePort cache,
      SemanticCachePayloadCodec codec,
      Clock clock,
      String promptVersion,
      Duration ttl,
      Optional<SemanticCacheHotStore> hotStore,
      SemanticMetrics metrics,
      EmbeddingModelConfiguration embeddingModelConfiguration,
      SemanticCacheIdentity identity,
      String locale,
      String quoteTemplateVersion,
      AiTraceRecorder traceRecorder) {
    this.legacyEmbeddings = Objects.requireNonNull(embeddings, "embeddings must not be null");
    this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
    this.cache = Objects.requireNonNull(cache, "cache must not be null");
    this.codec = Objects.requireNonNull(codec, "codec must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
    requireText(promptVersion, "promptVersion");
    this.promptVersion = promptVersion;
    this.ttl = requirePositive(ttl, "ttl");
    this.hotStore = Objects.requireNonNull(hotStore, "hotStore must not be null");
    this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
    this.embeddingModelConfiguration =
        Objects.requireNonNull(
            embeddingModelConfiguration, "embeddingModelConfiguration must not be null");
    this.identity = Objects.requireNonNull(identity, "identity must not be null");
    requireText(locale, "locale");
    requireText(quoteTemplateVersion, "quoteTemplateVersion");
    this.locale = locale;
    this.quoteTemplateVersion = quoteTemplateVersion;
    this.traceRecorder = Objects.requireNonNull(traceRecorder, "traceRecorder must not be null");
  }

  @Override
  public Optional<String> lookup(String conversationContext, SemanticQuery query) {
    if (!isEligible(conversationContext, query)) {
      recordLookup("bypass");
      return Optional.empty();
    }
    try {
      SemanticCachePort.Lookup lookup =
          new SemanticCachePort.Lookup(
              CACHE_KIND,
              contextFingerprint(conversationContext),
              promptVersion,
              query.embedding(),
              identityForCurrentContext());
      Optional<SemanticCachePort.Candidate> hotHit =
          hotStore
              .flatMap(store -> safeHotLookup(store, lookup, query.text()))
              .flatMap(candidates -> resolver.confirm(candidates, this::hasSafePayload));
      Optional<String> result =
          hotHit
              .or(() -> resolver.lookup(lookup, this::hasSafePayload))
              .flatMap(candidate -> codec.decodeText(candidate.responsePayload()));
      result = result.filter(SemanticChatCache::isSafeResponse);
      recordLookup(result.isPresent() ? "hit" : "miss");
      return result;
    } catch (RuntimeException failure) {
      SemanticFailurePolicy.rethrowSecurityFailure(failure);
      recordFailure("cache.lookup", failure);
      recordFallback("cache.lookup", fallbackReason(failure));
      recordSemanticFailure("cache.lookup", failure);
      recordLookup("failure");
      return Optional.empty();
    }
  }

  /**
   * @deprecated use {@link #lookup(String, SemanticQuery)}.
   */
  @Override
  @Deprecated
  public Optional<String> lookup(String conversationContext, String userMessage) {
    if (!isEligibleText(conversationContext, userMessage)) {
      recordLookup("bypass");
      return Optional.empty();
    }
    try {
      return lookup(
          conversationContext, new SemanticQuery(userMessage, legacyEmbeddings.embed(userMessage)));
    } catch (RuntimeException failure) {
      SemanticFailurePolicy.rethrowSecurityFailure(failure);
      recordFailure("cache.lookup", failure);
      recordFallback("cache.lookup", fallbackReason(failure));
      recordSemanticFailure("cache.lookup", failure);
      recordLookup("failure");
      return Optional.empty();
    }
  }

  @Override
  public Optional<UUID> store(String conversationContext, SemanticQuery query, String response) {
    return store(conversationContext, query, response, identityForCurrentContext());
  }

  /**
   * @deprecated use {@link #store(String, SemanticQuery, String)}.
   */
  @Override
  @Deprecated
  public Optional<UUID> store(String conversationContext, String userMessage, String response) {
    if (!isEligibleText(conversationContext, userMessage)) {
      recordWrite("bypass");
      return Optional.empty();
    }
    requireText(response, "response");
    if (!isSafeResponse(response)) {
      recordWrite("rejected");
      return Optional.empty();
    }
    try {
      return store(
          conversationContext,
          new SemanticQuery(userMessage, legacyEmbeddings.embed(userMessage)),
          response);
    } catch (RuntimeException failure) {
      SemanticFailurePolicy.rethrowSecurityFailure(failure);
      recordFailure("cache.store", failure);
      recordFallback("cache.store", fallbackReason(failure));
      recordSemanticFailure("cache.store", failure);
      recordWrite("failure");
      return Optional.empty();
    }
  }

  /**
   * @deprecated use {@link #store(String, SemanticQuery, String, SemanticCacheIdentity)}.
   */
  @Deprecated
  public Optional<UUID> store(
      String conversationContext,
      String userMessage,
      String response,
      SemanticCacheIdentity producingIdentity) {
    if (!isEligibleText(conversationContext, userMessage)) {
      recordWrite("bypass");
      return Optional.empty();
    }
    requireText(response, "response");
    if (!isSafeResponse(response)) {
      recordWrite("rejected");
      return Optional.empty();
    }
    try {
      return store(
          conversationContext,
          new SemanticQuery(userMessage, legacyEmbeddings.embed(userMessage)),
          response,
          producingIdentity);
    } catch (RuntimeException failure) {
      SemanticFailurePolicy.rethrowSecurityFailure(failure);
      recordFailure("cache.store", failure);
      recordFallback("cache.store", fallbackReason(failure));
      recordSemanticFailure("cache.store", failure);
      recordWrite("failure");
      return Optional.empty();
    }
  }

  @Override
  public Optional<UUID> store(
      String conversationContext,
      SemanticQuery query,
      String response,
      SemanticCacheIdentity producingIdentity) {
    if (!isEligible(conversationContext, query)) {
      recordWrite("bypass");
      return Optional.empty();
    }
    requireText(response, "response");
    if (!isSafeResponse(response)) {
      recordWrite("rejected");
      return Optional.empty();
    }
    try {
      String contextFingerprint = contextFingerprint(conversationContext);
      SemanticCacheIdentity cacheIdentity = mergeContextIdentity(producingIdentity);
      SemanticCachePort.Put write =
          new SemanticCachePort.Put(
              CACHE_KIND,
              query.text(),
              contextFingerprint,
              promptVersion,
              codec.encodeText(response),
              Instant.now(clock).plus(ttl),
              query.embedding(),
              writeIdempotencyKey(
                  contextFingerprint, query.text(), query.embedding(), cacheIdentity),
              cacheIdentity);
      UUID cacheId = cache.put(write);
      hotStore.ifPresent(store -> safeHotPut(store, cacheId, write));
      recordWrite("stored");
      return Optional.of(cacheId);
    } catch (RuntimeException failure) {
      SemanticFailurePolicy.rethrowSecurityFailure(failure);
      recordFailure("cache.store", failure);
      recordFallback("cache.store", fallbackReason(failure));
      recordSemanticFailure("cache.store", failure);
      recordWrite("failure");
      return Optional.empty();
    }
  }

  @Override
  public void invalidate() {
    AiExecutionContextScope.requireCurrent();
    cache.invalidate(CACHE_KIND);
  }

  private void recordLookup(String outcome) {
    try {
      metrics.recordCacheLookup(outcome);
    } catch (RuntimeException ignored) {
      // Observability must not change cache semantics.
    }
  }

  private boolean hasSafePayload(SemanticCachePort.Candidate candidate) {
    return codec
        .decodeText(candidate.responsePayload())
        .map(SemanticChatCache::isSafeResponse)
        .orElse(false);
  }

  private void recordWrite(String outcome) {
    try {
      metrics.recordCacheWrite(outcome);
    } catch (RuntimeException ignored) {
      // Observability must not change cache semantics.
    }
  }

  private Optional<List<SemanticCachePort.Candidate>> safeHotLookup(
      SemanticCacheHotStore store, SemanticCachePort.Lookup lookup, String queryText) {
    try {
      return Optional.of(store.find(lookup, queryText, 2));
    } catch (RuntimeException failure) {
      SemanticFailurePolicy.rethrowSecurityFailure(failure);
      recordFailure("cache.hot_lookup", failure);
      recordFallback("cache.hot_lookup", "hot_store_unavailable");
      return Optional.empty();
    }
  }

  private void safeHotPut(SemanticCacheHotStore store, UUID cacheId, SemanticCachePort.Put write) {
    try {
      store.put(cacheId, write);
    } catch (RuntimeException failure) {
      SemanticFailurePolicy.rethrowSecurityFailure(failure);
      recordFailure("cache.hot_store", failure);
      recordFallback("cache.hot_store", "hot_store_unavailable");
      // Redis is an optimization; durable PostgreSQL persistence has already succeeded.
    }
  }

  private void recordFailure(String operation, RuntimeException failure) {
    try {
      metrics.recordFailure(operation, SemanticFailureReason.code(failure));
    } catch (RuntimeException ignored) {
      // Observability must not change cache semantics.
    }
  }

  private void recordFallback(String operation, String reason) {
    try {
      metrics.recordFallback(operation, reason);
    } catch (RuntimeException ignored) {
      // Observability must not change cache semantics.
    }
  }

  private void recordSemanticFailure(String operation, RuntimeException failure) {
    try {
      traceRecorder.recordSemanticOutcome(
          new AiSemanticExecutionTrace(
              UUID.randomUUID(),
              null,
              null,
              operation,
              "failed",
              0.0,
              0.0,
              0.0,
              List.of(),
              null,
              null,
              null,
              0));
    } catch (RuntimeException traceFailure) {
      recordFailure("trace", traceFailure);
      AiTracePersistenceFailureReporter.report(LOGGER, operation, traceFailure);
    }
  }

  private static String fallbackReason(RuntimeException failure) {
    return failure instanceof EmbeddingProviderUnavailableException
        ? "embedding_unavailable"
        : "cache_unavailable";
  }

  private static boolean isEligible(String conversationContext, SemanticQuery query) {
    return query != null && isEligibleText(conversationContext, query.text());
  }

  private static boolean isEligibleText(String conversationContext, String userMessage) {
    if (conversationContext != null && !conversationContext.isBlank()) {
      return false;
    }
    if (userMessage == null || userMessage.isBlank()) {
      return false;
    }
    String normalized = userMessage.toLowerCase(java.util.Locale.ROOT);
    return java.util.stream.Stream.of(
            "book",
            "schedule",
            "reserve",
            "appointment",
            "availability",
            "available",
            "cancel",
            "reschedule",
            "price",
            "cost",
            "quote",
            "pay",
            "payment",
            "refund",
            "account")
        .noneMatch(normalized::contains);
  }

  private static boolean isSafeResponse(String response) {
    String normalized = response.toLowerCase(java.util.Locale.ROOT);
    return !normalized.matches(".*\\b(?:bearer\\s+)?[a-z0-9._-]{20,}\\b.*")
        && !normalized.matches(".*\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b.*")
        && !normalized.matches(".*\\b[\\w.+-]+@[\\w.-]+\\.[a-z]{2,}\\b.*")
        && !normalized.matches(".*(?:\\+?\\d[\\d ()-]{8,}\\d).*");
  }

  private static String contextFingerprint(String conversationContext) {
    String value = conversationContext == null ? "" : conversationContext;
    return "context-v1:" + sha256(value);
  }

  private String writeIdempotencyKey(
      String contextFingerprint,
      String userMessage,
      EmbeddingVector query,
      SemanticCacheIdentity cacheIdentity) {
    String embeddingIdentity =
        embeddingModelConfiguration.modelName()
            + "@"
            + query.model().version()
            + "#"
            + query.values().size();
    return promptVersion
        + ":"
        + sha256(
            contextFingerprint
                + "\u0000"
                + embeddingIdentity
                + "\u0000"
                + cacheIdentity
                + "\u0000"
                + userMessage);
  }

  private SemanticCacheIdentity identityForCurrentContext() {
    return mergeContextIdentity(identity);
  }

  private SemanticCacheIdentity mergeContextIdentity(SemanticCacheIdentity producingIdentity) {
    Objects.requireNonNull(producingIdentity, "producingIdentity must not be null");
    String channel =
        AiExecutionContextScope.current()
            .map(context -> context.channel().name())
            .orElse(producingIdentity.channel());
    return new SemanticCacheIdentity(
        producingIdentity.responseProvider(),
        producingIdentity.responseModel(),
        identity.knowledgeVersion(),
        identity.policyVersion(),
        identity.sourceVersion(),
        channel,
        locale,
        quoteTemplateVersion);
  }

  private static String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is required by the runtime", exception);
    }
  }

  private static Duration requirePositive(Duration value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(field + " must be positive");
    }
    return value;
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
