package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.EmbeddingProviderUnavailableException;
import com.emme.assistant.ai.application.port.out.SemanticCacheHotStore;
import com.emme.assistant.ai.application.port.out.SemanticCachePayloadCodec;
import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.semantic.EmbeddingVector;
import com.emme.assistant.ai.application.semantic.SemanticCacheIdentity;
import com.emme.assistant.ai.application.semantic.SemanticCachePolicy;
import com.emme.assistant.ai.application.semantic.SemanticCacheResolver;
import com.emme.assistant.ai.application.semantic.SemanticChatCache;
import com.emme.assistant.ai.application.trace.AiSemanticExecutionTrace;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.kernel.context.AiExecutionContextScope;
import com.emme.kernel.context.AiExecutionContext;
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

  private static final EmbeddingVector QUERY =
      new EmbeddingVector("embedding-v1", List.of(1.0f, 0.0f));

  @Test
  void returnsAValidatedCachedInformationalAnswer() {
    EmbeddingModelPort embeddings = mock(EmbeddingModelPort.class);
    SemanticCachePort cache = mock(SemanticCachePort.class);
    SemanticCachePayloadCodec codec = mock(SemanticCachePayloadCodec.class);
    UUID cacheId = UUID.randomUUID();
    when(embeddings.embed("What are your aftercare instructions?")).thenReturn(QUERY);
    when(cache.find(any(), any(Integer.class)))
        .thenReturn(List.of(new SemanticCachePort.Candidate(cacheId, "payload", 0.98)));
    when(cache.recordHit(cacheId)).thenReturn(true);
    when(codec.decodeText("payload")).thenReturn(Optional.of("Keep nails dry for 24 hours."));
    SemanticChatCache semanticCache =
        new SemanticChatCache(
            embeddings,
            new SemanticCacheResolver(cache, new SemanticCachePolicy(0.95)),
            cache,
            codec,
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5));

    assertThat(semanticCache.lookup("", "What are your aftercare instructions?"))
        .contains("Keep nails dry for 24 hours.");

    verify(cache).recordHit(cacheId);
  }

  @Test
  void bypassesTheCacheForTransactionalMessages() {
    EmbeddingModelPort embeddings = mock(EmbeddingModelPort.class);
    SemanticCachePort cache = mock(SemanticCachePort.class);
    SemanticChatCache semanticCache =
        new SemanticChatCache(
            embeddings,
            mock(SemanticCacheResolver.class),
            cache,
            mock(SemanticCachePayloadCodec.class),
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5));

    assertThat(semanticCache.lookup("", "Book me Friday at 5pm")).isEmpty();

    verifyNoInteractions(embeddings, cache);
  }

  @Test
  void storesOnlyEligibleResponsesWithAnExpiringHashedWriteKey() {
    EmbeddingModelPort embeddings = mock(EmbeddingModelPort.class);
    SemanticCachePort cache = mock(SemanticCachePort.class);
    SemanticCachePayloadCodec codec = mock(SemanticCachePayloadCodec.class);
    when(embeddings.embed("What are your hours?")).thenReturn(QUERY);
    when(codec.encodeText("We are open from 9 to 6."))
        .thenReturn("{\"text\":\"We are open from 9 to 6.\"}");
    UUID cacheId = UUID.randomUUID();
    when(cache.put(any())).thenReturn(cacheId);
    Clock clock = Clock.fixed(Instant.parse("2026-08-28T12:00:00Z"), ZoneOffset.UTC);
    SemanticChatCache semanticCache =
        new SemanticChatCache(
            embeddings,
            mock(SemanticCacheResolver.class),
            cache,
            codec,
            clock,
            "chat-v1",
            java.time.Duration.ofMinutes(5));

    assertThat(semanticCache.store("", "What are your hours?", "We are open from 9 to 6."))
        .contains(cacheId);

    var write = org.mockito.ArgumentCaptor.forClass(SemanticCachePort.Put.class);
    verify(cache).put(write.capture());
    assertThat(write.getValue().expiresAt()).isEqualTo(Instant.parse("2026-08-28T12:05:00Z"));
    assertThat(write.getValue().writeIdempotencyKey()).startsWith("chat-v1:");
  }

  @Test
  void includesConfiguredEmbeddingModelNameInTheCacheIdentity() {
    EmbeddingModelPort embeddings = mock(EmbeddingModelPort.class);
    SemanticCachePort cache = mock(SemanticCachePort.class);
    SemanticCachePayloadCodec codec = mock(SemanticCachePayloadCodec.class);
    when(embeddings.embed("What are your hours?")).thenReturn(QUERY);
    when(codec.encodeText(any())).thenReturn("payload");
    when(cache.put(any())).thenReturn(UUID.randomUUID());
    SemanticChatCache semanticCache =
        new SemanticChatCache(
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

    semanticCache.store("", "What are your hours?", "We are open.");
    SemanticChatCache otherModelCache =
        new SemanticChatCache(
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
    otherModelCache.store("", "What are your hours?", "We are open.");

    var write = org.mockito.ArgumentCaptor.forClass(SemanticCachePort.Put.class);
    verify(cache, org.mockito.Mockito.times(2)).put(write.capture());
    assertThat(write.getAllValues().get(0).writeIdempotencyKey())
        .isNotEqualTo(write.getAllValues().get(1).writeIdempotencyKey());
  }

  @Test
  void includesResponseProviderModelAndDependencyVersionsInTheCacheIdentity() {
    EmbeddingModelPort embeddings = mock(EmbeddingModelPort.class);
    SemanticCachePort cache = mock(SemanticCachePort.class);
    SemanticCachePayloadCodec codec = mock(SemanticCachePayloadCodec.class);
    when(embeddings.embed("What are your hours?")).thenReturn(QUERY);
    when(codec.encodeText(any())).thenReturn("payload");
    when(cache.put(any())).thenReturn(UUID.randomUUID());
    SemanticCacheIdentity identity =
        new SemanticCacheIdentity(
            "ollama", "gemma4:e4b-mlx", "knowledge-v7", "policy-v3", "source-v9");
    SemanticChatCache semanticCache =
        new SemanticChatCache(
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

    semanticCache.store("", "What are your hours?", "We are open.");

    var write = org.mockito.ArgumentCaptor.forClass(SemanticCachePort.Put.class);
    verify(cache).put(write.capture());
    assertThat(write.getValue().identity()).isEqualTo(identity);
  }

  @Test
  void includesChannelLocaleQuoteTemplateAndActualProducingModelInTheCacheIdentity() {
    EmbeddingModelPort embeddings = mock(EmbeddingModelPort.class);
    SemanticCachePort cache = mock(SemanticCachePort.class);
    SemanticCachePayloadCodec codec = mock(SemanticCachePayloadCodec.class);
    when(embeddings.embed("What are your hours?")).thenReturn(QUERY);
    when(codec.encodeText(any())).thenReturn("payload");
    UUID cacheId = UUID.randomUUID();
    when(cache.put(any())).thenReturn(cacheId);
    SemanticChatCache semanticCache =
        new SemanticChatCache(
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
                "", "What are your hours?", "We are open.",
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
    EmbeddingModelPort embeddings = mock(EmbeddingModelPort.class);
    SemanticCachePort cache = mock(SemanticCachePort.class);
    SemanticCachePayloadCodec codec = mock(SemanticCachePayloadCodec.class);
    when(embeddings.embed("What are your hours?"))
        .thenReturn(QUERY, new EmbeddingVector("embedding-v2", List.of(1.0f, 0.0f, 0.0f)));
    when(codec.encodeText(any())).thenReturn("payload");
    when(cache.put(any())).thenReturn(UUID.randomUUID());
    SemanticChatCache semanticCache =
        new SemanticChatCache(
            embeddings,
            mock(SemanticCacheResolver.class),
            cache,
            codec,
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5));

    semanticCache.store("", "What are your hours?", "We are open.");
    semanticCache.store("", "What are your hours?", "We are open.");

    var writes = org.mockito.ArgumentCaptor.forClass(SemanticCachePort.Put.class);
    verify(cache, org.mockito.Mockito.times(2)).put(writes.capture());
    assertThat(writes.getAllValues().get(0).writeIdempotencyKey())
        .isNotEqualTo(writes.getAllValues().get(1).writeIdempotencyKey());
  }

  @Test
  void confirmsAHotHitAgainstTheDurableCacheBeforeReturningIt() {
    EmbeddingModelPort embeddings = mock(EmbeddingModelPort.class);
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
        new SemanticChatCache(
            embeddings,
            new SemanticCacheResolver(durableCache, new SemanticCachePolicy(0.95)),
            durableCache,
            codec,
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5),
            Optional.of(hotStore));

    assertThat(semanticCache.lookup("", "What are your hours?"))
        .contains("We are open from 9 to 6.");

    org.mockito.Mockito.verify(durableCache).recordHit(cacheId);
    org.mockito.Mockito.verify(durableCache, org.mockito.Mockito.never()).find(any(), anyInt());
  }

  @Test
  void writesTheDurableEntryBeforeProjectingToTheHotStore() {
    EmbeddingModelPort embeddings = mock(EmbeddingModelPort.class);
    SemanticCachePort durableCache = mock(SemanticCachePort.class);
    SemanticCacheHotStore hotStore = mock(SemanticCacheHotStore.class);
    SemanticCachePayloadCodec codec = mock(SemanticCachePayloadCodec.class);
    UUID cacheId = UUID.randomUUID();
    when(embeddings.embed("What are your hours?")).thenReturn(QUERY);
    when(codec.encodeText("We are open.")).thenReturn("{\"text\":\"We are open.\"}");
    when(durableCache.put(any())).thenReturn(cacheId);
    SemanticChatCache semanticCache =
        new SemanticChatCache(
            embeddings,
            mock(SemanticCacheResolver.class),
            durableCache,
            codec,
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5),
            Optional.of(hotStore));

    assertThat(semanticCache.store("", "What are your hours?", "We are open.")).contains(cacheId);

    org.mockito.Mockito.verify(hotStore).put(org.mockito.Mockito.eq(cacheId), any());
  }

  @Test
  void doesNotStoreResponsesContainingPrivateOrPaymentData() {
    EmbeddingModelPort embeddings = mock(EmbeddingModelPort.class);
    SemanticCachePort durableCache = mock(SemanticCachePort.class);
    SemanticCachePayloadCodec codec = mock(SemanticCachePayloadCodec.class);
    SemanticChatCache semanticCache =
        new SemanticChatCache(
            embeddings,
            mock(SemanticCacheResolver.class),
            durableCache,
            codec,
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5));

    assertThat(semanticCache.store("", "What are your hours?", "Pay with card 4111 1111 1111 1111"))
        .isEmpty();

    verifyNoInteractions(embeddings, durableCache, codec);
  }

  @Test
  void rejectsAnUnsafePayloadAgainWhenAStoredEntryIsLookedUp() {
    EmbeddingModelPort embeddings = mock(EmbeddingModelPort.class);
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
        new SemanticChatCache(
            embeddings,
            new SemanticCacheResolver(durableCache, new SemanticCachePolicy(0.95)),
            durableCache,
            codec,
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5));

    assertThat(semanticCache.lookup("", "What are your hours?")).isEmpty();
    verify(durableCache, never()).recordHit(cacheId);
  }

  @Test
  void returnsAnEmptyResultWhenTheEmbeddingProviderIsUnavailable() {
    EmbeddingModelPort embeddings = mock(EmbeddingModelPort.class);
    SemanticCachePort durableCache = mock(SemanticCachePort.class);
    when(embeddings.embed("What are your hours?"))
        .thenThrow(new EmbeddingProviderUnavailableException("embedding unavailable"));
    SemanticChatCache semanticCache =
        new SemanticChatCache(
            embeddings,
            mock(SemanticCacheResolver.class),
            durableCache,
            mock(SemanticCachePayloadCodec.class),
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5));

    assertThat(semanticCache.lookup("", "What are your hours?")).isEmpty();
    verifyNoInteractions(durableCache);
  }

  @Test
  void recordsASemanticFailureWhilePreservingTheSafeEmptyFallback() {
    EmbeddingModelPort embeddings = mock(EmbeddingModelPort.class);
    SemanticCachePort durableCache = mock(SemanticCachePort.class);
    AiTraceRecorder traces = mock(AiTraceRecorder.class);
    when(embeddings.embed("What are your hours?"))
        .thenThrow(new EmbeddingProviderUnavailableException("embedding unavailable"));
    SemanticChatCache semanticCache =
        new SemanticChatCache(
            embeddings,
            mock(SemanticCacheResolver.class),
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

    assertThat(semanticCache.lookup("", "What are your hours?")).isEmpty();

    var trace = org.mockito.ArgumentCaptor.forClass(AiSemanticExecutionTrace.class);
    verify(traces).recordSemanticOutcome(trace.capture());
    assertThat(trace.getValue().outcome()).isEqualTo("failed");
  }

  @Test
  void returnsAnEmptyResultWhenTheDurableCacheIsUnavailable() {
    EmbeddingModelPort embeddings = mock(EmbeddingModelPort.class);
    SemanticCachePort durableCache = mock(SemanticCachePort.class);
    when(embeddings.embed("What are your hours?")).thenReturn(QUERY);
    when(durableCache.find(any(), anyInt())).thenThrow(new IllegalStateException("database down"));
    SemanticChatCache semanticCache =
        new SemanticChatCache(
            embeddings,
            new SemanticCacheResolver(durableCache, new SemanticCachePolicy(0.95, 0.05)),
            durableCache,
            mock(SemanticCachePayloadCodec.class),
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5));

    assertThat(semanticCache.lookup("", "What are your hours?")).isEmpty();
  }

  @Test
  void invalidatesOnlyTheCurrentPrincipalDurableCacheScope() {
    SemanticCachePort durableCache = mock(SemanticCachePort.class);
    SemanticChatCache semanticCache =
        new SemanticChatCache(
            mock(EmbeddingModelPort.class),
            mock(SemanticCacheResolver.class),
            durableCache,
            mock(SemanticCachePayloadCodec.class),
            Clock.systemUTC(),
            "chat-v1",
            java.time.Duration.ofMinutes(5));

    AiExecutionContextScope.run(context(), semanticCache::invalidate);

    org.mockito.Mockito.verify(durableCache).invalidate("CHAT_INFORMATIONAL");
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
