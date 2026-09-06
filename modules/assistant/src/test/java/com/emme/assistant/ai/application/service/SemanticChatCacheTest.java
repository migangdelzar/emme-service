package com.emme.assistant.ai.application.service;

import static com.emme.assistant.ai.EmbeddingTestVectors.testEmbedding;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.embedding.EmbeddingService;
import com.emme.ai.contracts.semantic.EmbeddingVector;
import com.emme.assistant.ai.application.port.out.NoopSemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticCacheHotStore;
import com.emme.assistant.ai.application.port.out.SemanticCachePayloadCodec;
import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.port.out.SemanticResponseCache;
import com.emme.assistant.ai.application.semantic.SemanticCacheIdentity;
import com.emme.assistant.ai.application.semantic.SemanticCachePolicy;
import com.emme.assistant.ai.application.semantic.SemanticCacheResolver;
import com.emme.assistant.ai.application.semantic.SemanticChatCache;
import com.emme.assistant.ai.application.semantic.SemanticQuery;
import com.emme.assistant.ai.application.trace.AiSemanticExecutionTrace;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.assistant.ai.application.trace.NoopAiTraceRecorder;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.emme.kernel.context.Channel;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SemanticChatCacheTest {

  private static final EmbeddingVector QUERY = testEmbedding("embedding-v1", List.of(1.0f, 0.0f));
  private static final SemanticQuery INFORMATIONAL_QUERY =
      new SemanticQuery("What are your hours?", QUERY);
  private static final SemanticQuery TRANSACTIONAL_QUERY =
      new SemanticQuery("Book me Friday at 5pm", QUERY);

  @Test
  void exposesOnlyPreparedSemanticCacheOperations() {
    assertThat(
            java.util.Arrays.stream(SemanticResponseCache.class.getMethods())
                .filter(
                    method -> method.getName().equals("lookup") || method.getName().equals("store"))
                .allMatch(
                    method ->
                        method.getParameterTypes().length < 2
                            || method.getParameterTypes()[1]
                                == com.emme.assistant.ai.application.semantic.SemanticQuery.class))
        .isTrue();
  }

  @Test
  void returnsAValidatedCachedInformationalAnswer() {
    EmbeddingService embeddings = mock(EmbeddingService.class);
    SemanticCachePort cache = mock(SemanticCachePort.class);
    SemanticCachePayloadCodec codec = mock(SemanticCachePayloadCodec.class);
    UUID cacheId = UUID.randomUUID();
    when(embeddings.embed("What are your aftercare instructions?")).thenReturn(QUERY);
    when(cache.find(any(), any(Integer.class)))
        .thenReturn(List.of(new SemanticCachePort.Candidate(cacheId, "payload", 0.98)));
    when(cache.recordHit(cacheId)).thenReturn(true);
    when(codec.decodeText("payload")).thenReturn(Optional.of("Keep nails dry for 24 hours."));
    SemanticChatCache semanticCache =
        cache(
            embeddings,
            new SemanticCacheResolver(cache, new SemanticCachePolicy(0.95)),
            cache,
            codec,
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5));

    assertThat(
            semanticCache.lookup(
                "", new SemanticQuery("What are your aftercare instructions?", QUERY)))
        .contains("Keep nails dry for 24 hours.");

    verify(cache).recordHit(cacheId);
  }

  @Test
  void bypassesTheCacheForTransactionalMessages() {
    EmbeddingService embeddings = mock(EmbeddingService.class);
    SemanticCachePort cache = mock(SemanticCachePort.class);
    SemanticChatCache semanticCache =
        cache(
            embeddings,
            mock(SemanticCacheResolver.class),
            cache,
            mock(SemanticCachePayloadCodec.class),
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5));

    assertThat(semanticCache.lookup("", TRANSACTIONAL_QUERY)).isEmpty();

    verifyNoInteractions(embeddings, cache);
  }

  @Test
  void storesOnlyEligibleResponsesWithAnExpiringHashedWriteKey() {
    EmbeddingService embeddings = mock(EmbeddingService.class);
    SemanticCachePort cache = mock(SemanticCachePort.class);
    SemanticCachePayloadCodec codec = mock(SemanticCachePayloadCodec.class);
    when(embeddings.embed("What are your hours?")).thenReturn(QUERY);
    when(codec.encodeText("We are open from 9 to 6."))
        .thenReturn("{\"text\":\"We are open from 9 to 6.\"}");
    UUID cacheId = UUID.randomUUID();
    when(cache.put(any())).thenReturn(cacheId);
    Clock clock = Clock.fixed(Instant.parse("2026-08-28T12:00:00Z"), ZoneOffset.UTC);
    SemanticChatCache semanticCache =
        cache(
            embeddings,
            mock(SemanticCacheResolver.class),
            cache,
            codec,
            clock,
            "chat-v1",
            java.time.Duration.ofMinutes(5));

    assertThat(semanticCache.store("", INFORMATIONAL_QUERY, "We are open from 9 to 6."))
        .contains(cacheId);

    var write = org.mockito.ArgumentCaptor.forClass(SemanticCachePort.Put.class);
    verify(cache).put(write.capture());
    assertThat(write.getValue().expiresAt()).isEqualTo(Instant.parse("2026-08-28T12:05:00Z"));
    assertThat(write.getValue().writeIdempotencyKey()).startsWith("chat-v1:");
  }

  @Test
  void includesConfiguredEmbeddingModelNameInTheCacheIdentity() {
    EmbeddingService embeddings = mock(EmbeddingService.class);
    SemanticCachePort cache = mock(SemanticCachePort.class);
    SemanticCachePayloadCodec codec = mock(SemanticCachePayloadCodec.class);
    when(embeddings.embed("What are your hours?")).thenReturn(QUERY);
    when(codec.encodeText(any())).thenReturn("payload");
    when(cache.put(any())).thenReturn(UUID.randomUUID());
    SemanticChatCache semanticCache =
        cache(
            embeddings,
            mock(SemanticCacheResolver.class),
            cache,
            codec,
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5),
            Optional.empty(),
            mock(com.emme.assistant.ai.application.port.out.SemanticMetrics.class),
            new com.emme.ai.contracts.semantic.EmbeddingModelConfiguration(
                "custom-embedding", "embedding-v1", 2));

    semanticCache.store("", INFORMATIONAL_QUERY, "We are open.");
    SemanticChatCache otherModelCache =
        cache(
            embeddings,
            mock(SemanticCacheResolver.class),
            cache,
            codec,
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5),
            Optional.empty(),
            mock(com.emme.assistant.ai.application.port.out.SemanticMetrics.class),
            new com.emme.ai.contracts.semantic.EmbeddingModelConfiguration(
                "other-embedding", "embedding-v1", 2));
    otherModelCache.store("", INFORMATIONAL_QUERY, "We are open.");

    var write = org.mockito.ArgumentCaptor.forClass(SemanticCachePort.Put.class);
    verify(cache, org.mockito.Mockito.times(2)).put(write.capture());
    assertThat(write.getAllValues().get(0).writeIdempotencyKey())
        .isNotEqualTo(write.getAllValues().get(1).writeIdempotencyKey());
  }

  @Test
  void includesResponseProviderModelAndDependencyVersionsInTheCacheIdentity() {
    EmbeddingService embeddings = mock(EmbeddingService.class);
    SemanticCachePort cache = mock(SemanticCachePort.class);
    SemanticCachePayloadCodec codec = mock(SemanticCachePayloadCodec.class);
    when(embeddings.embed("What are your hours?")).thenReturn(QUERY);
    when(codec.encodeText(any())).thenReturn("payload");
    when(cache.put(any())).thenReturn(UUID.randomUUID());
    SemanticCacheIdentity identity =
        new SemanticCacheIdentity(
            "ollama", "gemma4:e4b-mlx", "knowledge-v7", "policy-v3", "source-v9");
    SemanticChatCache semanticCache =
        cache(
            embeddings,
            mock(SemanticCacheResolver.class),
            cache,
            codec,
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5),
            Optional.empty(),
            mock(com.emme.assistant.ai.application.port.out.SemanticMetrics.class),
            new com.emme.ai.contracts.semantic.EmbeddingModelConfiguration(
                "custom-embedding", "embedding-v1", 2),
            identity);

    semanticCache.store("", INFORMATIONAL_QUERY, "We are open.");

    var write = org.mockito.ArgumentCaptor.forClass(SemanticCachePort.Put.class);
    verify(cache).put(write.capture());
    assertThat(write.getValue().identity()).isEqualTo(identity);
  }

  @Test
  void includesChannelLocaleQuoteTemplateAndActualProducingModelInTheCacheIdentity() {
    EmbeddingService embeddings = mock(EmbeddingService.class);
    SemanticCachePort cache = mock(SemanticCachePort.class);
    SemanticCachePayloadCodec codec = mock(SemanticCachePayloadCodec.class);
    when(embeddings.embed("What are your hours?")).thenReturn(QUERY);
    when(codec.encodeText(any())).thenReturn("payload");
    UUID cacheId = UUID.randomUUID();
    when(cache.put(any())).thenReturn(cacheId);
    SemanticChatCache semanticCache =
        cache(
            embeddings,
            mock(SemanticCacheResolver.class),
            cache,
            codec,
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5),
            Optional.empty(),
            mock(com.emme.assistant.ai.application.port.out.SemanticMetrics.class),
            new com.emme.ai.contracts.semantic.EmbeddingModelConfiguration(
                "custom-embedding", "embedding-v1", 2),
            new SemanticCacheIdentity(
                "configured-provider",
                "configured-model",
                "knowledge-v7",
                "policy-v3",
                "source-v9"),
            "es-US",
            "quote-template-v4");

    AiExecutionContext context =
        new com.emme.kernel.context.AiExecutionContext(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Set.of("client"),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "trace-channel",
            "turn-channel",
            Channel.WHATSAPP,
            Set.of("appointments"),
            Set.of("ai_chat"));
    AiExecutionContextScope.run(
        context,
        () ->
            semanticCache.store(
                "",
                INFORMATIONAL_QUERY,
                "We are open.",
                new SemanticCacheIdentity(
                    "ollama", "gemma4:e4b-mlx", "knowledge-v7", "policy-v3", "source-v9")));

    var write = org.mockito.ArgumentCaptor.forClass(SemanticCachePort.Put.class);
    verify(cache).put(write.capture());
    assertThat(write.getValue().identity().responseProvider()).isEqualTo("ollama");
    assertThat(write.getValue().identity().responseModel()).isEqualTo("gemma4:e4b-mlx");
    assertThat(write.getValue().identity().channel()).isEqualTo("WHATSAPP");
    assertThat(write.getValue().identity().locale()).isEqualTo("es-US");
    assertThat(write.getValue().identity().quoteTemplateVersion()).isEqualTo("quote-template-v4");
  }

  @Test
  void separatesCacheWritesByEmbeddingModelAndDimension() {
    EmbeddingService embeddings = mock(EmbeddingService.class);
    SemanticCachePort cache = mock(SemanticCachePort.class);
    SemanticCachePayloadCodec codec = mock(SemanticCachePayloadCodec.class);
    when(embeddings.embed("What are your hours?"))
        .thenReturn(QUERY, testEmbedding("embedding-v2", List.of(1.0f, 0.0f, 0.0f)));
    when(codec.encodeText(any())).thenReturn("payload");
    when(cache.put(any())).thenReturn(UUID.randomUUID());
    SemanticChatCache semanticCache =
        cache(
            embeddings,
            mock(SemanticCacheResolver.class),
            cache,
            codec,
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5));

    semanticCache.store("", INFORMATIONAL_QUERY, "We are open.");
    semanticCache.store(
        "",
        new SemanticQuery(
            "What are your hours?", testEmbedding("embedding-v2", List.of(1.0f, 0.0f, 0.0f))),
        "We are open.");

    var writes = org.mockito.ArgumentCaptor.forClass(SemanticCachePort.Put.class);
    verify(cache, org.mockito.Mockito.times(2)).put(writes.capture());
    assertThat(writes.getAllValues().get(0).writeIdempotencyKey())
        .isNotEqualTo(writes.getAllValues().get(1).writeIdempotencyKey());
  }

  @Test
  void confirmsAHotHitAgainstTheDurableCacheBeforeReturningIt() {
    EmbeddingService embeddings = mock(EmbeddingService.class);
    SemanticCachePort durableCache = mock(SemanticCachePort.class);
    SemanticCacheHotStore hotStore = mock(SemanticCacheHotStore.class);
    SemanticCachePayloadCodec codec = mock(SemanticCachePayloadCodec.class);
    UUID cacheId = UUID.randomUUID();
    when(embeddings.embed("What are your hours?")).thenReturn(QUERY);
    when(hotStore.find(any(), any(), anyInt()))
        .thenReturn(List.of(new SemanticCachePort.Candidate(cacheId, "payload", 0.99)));
    when(durableCache.recordHit(cacheId)).thenReturn(true);
    when(codec.decodeText("payload")).thenReturn(Optional.of("We are open from 9 to 6."));
    SemanticChatCache semanticCache =
        cache(
            embeddings,
            new SemanticCacheResolver(durableCache, new SemanticCachePolicy(0.95)),
            durableCache,
            codec,
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5),
            Optional.of(hotStore));

    assertThat(semanticCache.lookup("", INFORMATIONAL_QUERY)).contains("We are open from 9 to 6.");

    org.mockito.Mockito.verify(durableCache).recordHit(cacheId);
    org.mockito.Mockito.verify(durableCache, org.mockito.Mockito.never()).find(any(), anyInt());
  }

  @Test
  void fallsBackToTheDurableCacheWhenTheHotProjectionIsUnavailable() {
    EmbeddingService embeddings = mock(EmbeddingService.class);
    SemanticCachePort durableCache = mock(SemanticCachePort.class);
    SemanticCacheHotStore hotStore = mock(SemanticCacheHotStore.class);
    SemanticCachePayloadCodec codec = mock(SemanticCachePayloadCodec.class);
    UUID cacheId = UUID.randomUUID();
    when(embeddings.embed("What are your hours?")).thenReturn(QUERY);
    when(hotStore.find(any(), any(), anyInt())).thenThrow(new IllegalStateException("Redis down"));
    when(durableCache.find(any(), anyInt()))
        .thenReturn(List.of(new SemanticCachePort.Candidate(cacheId, "payload", 0.99)));
    when(durableCache.recordHit(cacheId)).thenReturn(true);
    when(codec.decodeText("payload")).thenReturn(Optional.of("We are open from 9 to 6."));
    SemanticChatCache semanticCache =
        cache(
            embeddings,
            new SemanticCacheResolver(durableCache, new SemanticCachePolicy(0.95)),
            durableCache,
            codec,
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5),
            Optional.of(hotStore));

    assertThat(semanticCache.lookup("", INFORMATIONAL_QUERY)).contains("We are open from 9 to 6.");

    verify(durableCache).find(any(), anyInt());
    verify(durableCache).recordHit(cacheId);
  }

  @Test
  void writesTheDurableEntryBeforeProjectingToTheHotStore() {
    EmbeddingService embeddings = mock(EmbeddingService.class);
    SemanticCachePort durableCache = mock(SemanticCachePort.class);
    SemanticCacheHotStore hotStore = mock(SemanticCacheHotStore.class);
    SemanticCachePayloadCodec codec = mock(SemanticCachePayloadCodec.class);
    UUID cacheId = UUID.randomUUID();
    when(embeddings.embed("What are your hours?")).thenReturn(QUERY);
    when(codec.encodeText("We are open.")).thenReturn("{\"text\":\"We are open.\"}");
    when(durableCache.put(any())).thenReturn(cacheId);
    SemanticChatCache semanticCache =
        cache(
            embeddings,
            new SemanticCacheResolver(durableCache, new SemanticCachePolicy(0.95, 0.05)),
            durableCache,
            codec,
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5),
            Optional.of(hotStore));

    assertThat(semanticCache.store("", INFORMATIONAL_QUERY, "We are open.")).contains(cacheId);

    org.mockito.Mockito.verify(hotStore).put(org.mockito.Mockito.eq(cacheId), any());
  }

  @Test
  void doesNotStoreResponsesContainingPrivateOrPaymentData() {
    EmbeddingService embeddings = mock(EmbeddingService.class);
    SemanticCachePort durableCache = mock(SemanticCachePort.class);
    SemanticCachePayloadCodec codec = mock(SemanticCachePayloadCodec.class);
    SemanticChatCache semanticCache =
        cache(
            embeddings,
            mock(SemanticCacheResolver.class),
            durableCache,
            codec,
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5));

    assertThat(semanticCache.store("", INFORMATIONAL_QUERY, "Pay with card 4111 1111 1111 1111"))
        .isEmpty();

    verifyNoInteractions(embeddings, durableCache, codec);
  }

  @Test
  void rejectsAnUnsafePayloadAgainWhenAStoredEntryIsLookedUp() {
    EmbeddingService embeddings = mock(EmbeddingService.class);
    SemanticCachePort durableCache = mock(SemanticCachePort.class);
    SemanticCachePayloadCodec codec = mock(SemanticCachePayloadCodec.class);
    UUID cacheId = UUID.randomUUID();
    when(embeddings.embed("What are your hours?")).thenReturn(QUERY);
    when(durableCache.find(any(), anyInt()))
        .thenReturn(List.of(new SemanticCachePort.Candidate(cacheId, "payload", 0.99)));
    when(durableCache.recordHit(cacheId)).thenReturn(true);
    when(codec.decodeText("payload"))
        .thenReturn(Optional.of("Contact client@example.com for your private details."));
    SemanticChatCache semanticCache =
        cache(
            embeddings,
            new SemanticCacheResolver(durableCache, new SemanticCachePolicy(0.95)),
            durableCache,
            codec,
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5));

    assertThat(semanticCache.lookup("", INFORMATIONAL_QUERY)).isEmpty();
    verify(durableCache, never()).recordHit(cacheId);
  }

  @Test
  void returnsAnEmptyResultWhenThePreparedQueryIsMissing() {
    EmbeddingService embeddings = mock(EmbeddingService.class);
    SemanticCachePort durableCache = mock(SemanticCachePort.class);
    SemanticChatCache semanticCache =
        cache(
            embeddings,
            mock(SemanticCacheResolver.class),
            durableCache,
            mock(SemanticCachePayloadCodec.class),
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5));

    assertThat(semanticCache.lookup("", null)).isEmpty();
    verifyNoInteractions(durableCache);
  }

  @Test
  void recordsASemanticFailureWhilePreservingTheSafeEmptyFallback() {
    EmbeddingService embeddings = mock(EmbeddingService.class);
    SemanticCachePort durableCache = mock(SemanticCachePort.class);
    AiTraceRecorder traces = mock(AiTraceRecorder.class);
    when(durableCache.find(any(), anyInt()))
        .thenThrow(new IllegalStateException("database unavailable"));
    SemanticChatCache semanticCache =
        cache(
            embeddings,
            new SemanticCacheResolver(durableCache, new SemanticCachePolicy(0.95, 0.05)),
            durableCache,
            mock(SemanticCachePayloadCodec.class),
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5),
            Optional.empty(),
            mock(com.emme.assistant.ai.application.port.out.SemanticMetrics.class),
            new com.emme.ai.contracts.semantic.EmbeddingModelConfiguration("embedding", "v1", 2),
            SemanticCacheIdentity.legacy(),
            "es-MX",
            "quote-template-v1",
            traces);

    assertThat(semanticCache.lookup("", INFORMATIONAL_QUERY)).isEmpty();

    var trace = org.mockito.ArgumentCaptor.forClass(AiSemanticExecutionTrace.class);
    verify(traces).recordSemanticOutcome(trace.capture());
    assertThat(trace.getValue().outcome()).isEqualTo("failed");
  }

  @Test
  void returnsAnEmptyResultWhenTheDurableCacheIsUnavailable() {
    EmbeddingService embeddings = mock(EmbeddingService.class);
    SemanticCachePort durableCache = mock(SemanticCachePort.class);
    when(embeddings.embed("What are your hours?")).thenReturn(QUERY);
    when(durableCache.find(any(), anyInt())).thenThrow(new IllegalStateException("database down"));
    SemanticChatCache semanticCache =
        cache(
            embeddings,
            new SemanticCacheResolver(durableCache, new SemanticCachePolicy(0.95, 0.05)),
            durableCache,
            mock(SemanticCachePayloadCodec.class),
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5));

    assertThat(semanticCache.lookup("", INFORMATIONAL_QUERY)).isEmpty();
  }

  @Test
  void invalidatesOnlyTheCurrentPrincipalDurableCacheScope() {
    SemanticCachePort durableCache = mock(SemanticCachePort.class);
    SemanticChatCache semanticCache =
        cache(
            mock(EmbeddingService.class),
            mock(SemanticCacheResolver.class),
            durableCache,
            mock(SemanticCachePayloadCodec.class),
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5));

    AiExecutionContextScope.run(context(), semanticCache::invalidate);

    org.mockito.Mockito.verify(durableCache).invalidate("CHAT_INFORMATIONAL");
  }

  private static SemanticChatCache cache(
      EmbeddingService embeddings,
      SemanticCacheResolver resolver,
      SemanticCachePort durableCache,
      SemanticCachePayloadCodec codec,
      Clock clock,
      String promptVersion,
      java.time.Duration ttl) {
    return cache(
        embeddings,
        resolver,
        durableCache,
        codec,
        clock,
        promptVersion,
        ttl,
        Optional.empty(),
        NoopSemanticMetrics.INSTANCE,
        new com.emme.ai.contracts.semantic.EmbeddingModelConfiguration(
            "embedding", "embedding-v1", 2),
        SemanticCacheIdentity.legacy(),
        "es-MX",
        "quote-template-v1",
        NoopAiTraceRecorder.INSTANCE);
  }

  private static SemanticChatCache cache(
      EmbeddingService embeddings,
      SemanticCacheResolver resolver,
      SemanticCachePort durableCache,
      SemanticCachePayloadCodec codec,
      Clock clock,
      String promptVersion,
      java.time.Duration ttl,
      Optional<SemanticCacheHotStore> hotStore) {
    return cache(
        embeddings,
        resolver,
        durableCache,
        codec,
        clock,
        promptVersion,
        ttl,
        hotStore,
        NoopSemanticMetrics.INSTANCE,
        new com.emme.ai.contracts.semantic.EmbeddingModelConfiguration(
            "embedding", "embedding-v1", 2),
        SemanticCacheIdentity.legacy(),
        "es-MX",
        "quote-template-v1",
        NoopAiTraceRecorder.INSTANCE);
  }

  private static SemanticChatCache cache(
      EmbeddingService embeddings,
      SemanticCacheResolver resolver,
      SemanticCachePort durableCache,
      SemanticCachePayloadCodec codec,
      Clock clock,
      String promptVersion,
      java.time.Duration ttl,
      Optional<SemanticCacheHotStore> hotStore,
      com.emme.assistant.ai.application.port.out.SemanticMetrics metrics,
      com.emme.ai.contracts.semantic.EmbeddingModelConfiguration embeddingConfiguration) {
    return cache(
        embeddings,
        resolver,
        durableCache,
        codec,
        clock,
        promptVersion,
        ttl,
        hotStore,
        metrics,
        embeddingConfiguration,
        SemanticCacheIdentity.legacy(),
        "es-MX",
        "quote-template-v1",
        NoopAiTraceRecorder.INSTANCE);
  }

  private static SemanticChatCache cache(
      EmbeddingService embeddings,
      SemanticCacheResolver resolver,
      SemanticCachePort durableCache,
      SemanticCachePayloadCodec codec,
      Clock clock,
      String promptVersion,
      java.time.Duration ttl,
      Optional<SemanticCacheHotStore> hotStore,
      com.emme.assistant.ai.application.port.out.SemanticMetrics metrics,
      com.emme.ai.contracts.semantic.EmbeddingModelConfiguration embeddingConfiguration,
      SemanticCacheIdentity identity) {
    return cache(
        embeddings,
        resolver,
        durableCache,
        codec,
        clock,
        promptVersion,
        ttl,
        hotStore,
        metrics,
        embeddingConfiguration,
        identity,
        "es-MX",
        "quote-template-v1",
        NoopAiTraceRecorder.INSTANCE);
  }

  private static SemanticChatCache cache(
      EmbeddingService embeddings,
      SemanticCacheResolver resolver,
      SemanticCachePort durableCache,
      SemanticCachePayloadCodec codec,
      Clock clock,
      String promptVersion,
      java.time.Duration ttl,
      Optional<SemanticCacheHotStore> hotStore,
      com.emme.assistant.ai.application.port.out.SemanticMetrics metrics,
      com.emme.ai.contracts.semantic.EmbeddingModelConfiguration embeddingConfiguration,
      SemanticCacheIdentity identity,
      String locale,
      String quoteTemplateVersion) {
    return cache(
        embeddings,
        resolver,
        durableCache,
        codec,
        clock,
        promptVersion,
        ttl,
        hotStore,
        metrics,
        embeddingConfiguration,
        identity,
        locale,
        quoteTemplateVersion,
        NoopAiTraceRecorder.INSTANCE);
  }

  private static SemanticChatCache cache(
      EmbeddingService embeddings,
      SemanticCacheResolver resolver,
      SemanticCachePort durableCache,
      SemanticCachePayloadCodec codec,
      Clock clock,
      String promptVersion,
      java.time.Duration ttl,
      Optional<SemanticCacheHotStore> hotStore,
      com.emme.assistant.ai.application.port.out.SemanticMetrics metrics,
      com.emme.ai.contracts.semantic.EmbeddingModelConfiguration embeddingConfiguration,
      SemanticCacheIdentity identity,
      String locale,
      String quoteTemplateVersion,
      AiTraceRecorder traceRecorder) {
    return new SemanticChatCache(
        resolver,
        durableCache,
        codec,
        clock,
        promptVersion,
        ttl,
        hotStore,
        metrics,
        embeddingConfiguration,
        identity,
        locale,
        quoteTemplateVersion,
        traceRecorder);
  }

  private static com.emme.kernel.context.AiExecutionContext context() {
    UUID id = UUID.randomUUID();
    return new com.emme.kernel.context.AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        java.util.Set.of("ROLE_CLIENT"),
        id,
        id,
        "trace",
        "id");
  }
}
