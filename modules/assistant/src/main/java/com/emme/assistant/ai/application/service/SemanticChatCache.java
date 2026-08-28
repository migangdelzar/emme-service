package com.emme.assistant.ai.application.service;

import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.SemanticCachePayloadCodec;
import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.port.out.SemanticResponseCache;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Principal-scoped semantic cache for safe informational chat responses.
 *
 * <p>Transactional requests never enter this cache. PostgreSQL remains authoritative for the cached
 * response and hit accounting; this service only defines eligibility and cache identity.
 */
public final class SemanticChatCache implements SemanticResponseCache {

  private static final String CACHE_KIND = "CHAT_INFORMATIONAL";

  private final EmbeddingModelPort embeddings;
  private final SemanticCacheResolver resolver;
  private final SemanticCachePort cache;
  private final SemanticCachePayloadCodec codec;
  private final Clock clock;
  private final String promptVersion;
  private final Duration ttl;

  public SemanticChatCache(
      EmbeddingModelPort embeddings,
      SemanticCacheResolver resolver,
      SemanticCachePort cache,
      SemanticCachePayloadCodec codec,
      Clock clock,
      String promptVersion,
      Duration ttl) {
    this.embeddings = Objects.requireNonNull(embeddings, "embeddings must not be null");
    this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
    this.cache = Objects.requireNonNull(cache, "cache must not be null");
    this.codec = Objects.requireNonNull(codec, "codec must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
    requireText(promptVersion, "promptVersion");
    this.promptVersion = promptVersion;
    this.ttl = requirePositive(ttl, "ttl");
  }

  @Override
  public Optional<String> lookup(String conversationContext, String userMessage) {
    if (!isEligible(conversationContext, userMessage)) {
      return Optional.empty();
    }
    EmbeddingVector query = embeddings.embed(userMessage);
    SemanticCachePort.Lookup lookup =
        new SemanticCachePort.Lookup(
            CACHE_KIND, contextFingerprint(conversationContext), promptVersion, query);
    return resolver
        .lookup(lookup)
        .flatMap(candidate -> codec.decodeText(candidate.responsePayload()));
  }

  @Override
  public Optional<UUID> store(String conversationContext, String userMessage, String response) {
    if (!isEligible(conversationContext, userMessage)) {
      return Optional.empty();
    }
    requireText(response, "response");
    EmbeddingVector query = embeddings.embed(userMessage);
    String contextFingerprint = contextFingerprint(conversationContext);
    SemanticCachePort.Put write =
        new SemanticCachePort.Put(
            CACHE_KIND,
            userMessage,
            contextFingerprint,
            promptVersion,
            codec.encodeText(response),
            Instant.now(clock).plus(ttl),
            query,
            writeIdempotencyKey(contextFingerprint, userMessage));
    return Optional.of(cache.put(write));
  }

  private static boolean isEligible(String conversationContext, String userMessage) {
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

  private static String contextFingerprint(String conversationContext) {
    String value = conversationContext == null ? "" : conversationContext;
    return "context-v1:" + sha256(value);
  }

  private String writeIdempotencyKey(String contextFingerprint, String userMessage) {
    return promptVersion + ":" + sha256(contextFingerprint + "\u0000" + userMessage);
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
