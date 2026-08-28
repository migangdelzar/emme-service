package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.SemanticCachePayloadCodec;
import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.semantic.EmbeddingVector;
import com.emme.assistant.ai.application.semantic.SemanticCachePolicy;
import com.emme.assistant.ai.application.semantic.SemanticCacheResolver;
import com.emme.assistant.ai.application.semantic.SemanticChatCache;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
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
}
