package com.emme.assistant.ai.adapter.out.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.semantic.SemanticCacheDependencyChanged;
import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.semantic.EmbeddingVector;
import com.emme.assistant.ai.application.semantic.SemanticCacheIdentity;
import com.emme.assistant.ai.application.semantic.SemanticCacheInvalidation;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import redis.clients.jedis.RedisClient;

class RedisSemanticCacheHotStoreTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID PRINCIPAL_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();
  private static final EmbeddingVector QUERY =
      new EmbeddingVector("embeddinggemma-v1", List.of(1.0f, 0.0f));

  @Test
  void readsAHotCandidateUsingTheQueryAndTheBackendTenantFilter() {
    VectorStore vectorStore = mock(VectorStore.class);
    UUID durableId = UUID.randomUUID();
    Document document =
        Document.builder()
            .id("hot-entry")
            .text("What are your hours?")
            .metadata(
                Map.of(
                    "durableCacheId", durableId.toString(),
                    "responsePayload", "{\"text\":\"We are open.\"}",
                    "expiresAt", Instant.now().plusSeconds(60).getEpochSecond()))
            .score(0.98)
            .build();
    when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document));
    RedisSemanticCacheHotStore hotStore =
        new RedisSemanticCacheHotStore(vectorStore, "embeddinggemma-v1", 2);

    List<SemanticCachePort.Candidate> result =
        AiExecutionContextScope.call(
            context(),
            () ->
                hotStore.find(
                    new SemanticCachePort.Lookup("CHAT_INFORMATIONAL", "ctx", "chat-v1", QUERY),
                    "What are your hours?",
                    2));

    assertThat(result)
        .singleElement()
        .satisfies(
            candidate -> {
              assertThat(candidate.id()).isEqualTo(durableId);
              assertThat(candidate.responsePayload()).contains("We are open");
              assertThat(candidate.similarity()).isEqualTo(0.98);
            });
    verify(vectorStore).similaritySearch(any(SearchRequest.class));
  }

  @Test
  void projectsDurableEntryWithBackendScopeMetadata() {
    VectorStore vectorStore = mock(VectorStore.class);
    UUID durableId = UUID.randomUUID();
    RedisSemanticCacheHotStore hotStore =
        new RedisSemanticCacheHotStore(vectorStore, "embeddinggemma-v1", 2);
    SemanticCacheIdentity identity =
        new SemanticCacheIdentity(
            "ollama", "gemma4:e4b-mlx", "knowledge-v7", "policy-v3", "source-v9");
    SemanticCachePort.Put write =
        new SemanticCachePort.Put(
            "CHAT_INFORMATIONAL",
            "What are your hours?",
            "ctx",
            "chat-v1",
            "{\"text\":\"We are open.\"}",
            Instant.now().plusSeconds(60),
            QUERY,
            "write-key",
            identity);

    AiExecutionContextScope.run(context(), () -> hotStore.put(durableId, write));

    AtomicReference<Document> documentReference = new AtomicReference<>();
    verify(vectorStore)
        .add(
            argThat(
                documents -> {
                  documentReference.set(documents.getFirst());
                  return true;
                }));
    Document document = documentReference.get();
    assertThat(document.getText()).isEqualTo(write.queryText());
    assertThat(document.getMetadata())
        .containsEntry("tenantId", encodeTagValue(TENANT_ID.toString()))
        .containsEntry("principalId", encodeTagValue(PRINCIPAL_ID.toString()))
        .containsEntry("durableCacheId", durableId.toString())
        .containsEntry("embeddingModelName", encodeTagValue("embeddinggemma:300m"))
        .containsEntry("responseProvider", encodeTagValue(identity.responseProvider()))
        .containsEntry("responseModel", encodeTagValue(identity.responseModel()))
        .containsEntry("knowledgeVersion", encodeTagValue(identity.knowledgeVersion()))
        .containsEntry("policyVersion", encodeTagValue(identity.policyVersion()))
        .containsEntry("sourceVersion", encodeTagValue(identity.sourceVersion()));
  }

  @Test
  void appliesTheDurableExpiryToTheRedisProjectionKey() {
    VectorStore vectorStore = mock(VectorStore.class);
    RedisClient redisClient = mock(RedisClient.class);
    Clock clock = Clock.fixed(Instant.parse("2026-08-29T12:00:00Z"), ZoneOffset.UTC);
    UUID durableId = UUID.randomUUID();
    RedisSemanticCacheHotStore hotStore =
        new RedisSemanticCacheHotStore(
            vectorStore, "embeddinggemma-v1", 2, clock, redisClient, "emme:ai:semantic-cache:");
    SemanticCachePort.Put write =
        new SemanticCachePort.Put(
            "CHAT_INFORMATIONAL",
            "What are your hours?",
            "ctx",
            "chat-v1",
            "{\"text\":\"We are open.\"}",
            Instant.parse("2026-08-29T12:01:30Z"),
            QUERY,
            "write-key");

    AiExecutionContextScope.run(context(), () -> hotStore.put(durableId, write));

    org.mockito.Mockito.verify(redisClient)
        .expire("emme:ai:semantic-cache:cache-" + durableId, 90L);
  }

  @Test
  void rejectsHotOperationsWithoutTheBackendContext() {
    RedisSemanticCacheHotStore hotStore =
        new RedisSemanticCacheHotStore(mock(VectorStore.class), "embeddinggemma-v1", 2);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                hotStore.find(
                    new SemanticCachePort.Lookup("CHAT_INFORMATIONAL", "ctx", "chat-v1", QUERY),
                    "hours",
                    1))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  @Test
  void invalidatesOnlyTheTargetPrincipalProjectionKeys() {
    VectorStore vectorStore = mock(VectorStore.class);
    RedisClient redisClient = mock(RedisClient.class);
    when(redisClient.smembers("prefix:tenant:" + TENANT_ID))
        .thenReturn(Set.of("prefix:tenant:" + TENANT_ID + ":principal:" + PRINCIPAL_ID));
    String documentKey = "prefix:cache-" + UUID.randomUUID();
    when(redisClient.smembers("prefix:tenant:" + TENANT_ID + ":principal:" + PRINCIPAL_ID))
        .thenReturn(Set.of(documentKey));
    RedisSemanticCacheHotStore hotStore =
        new RedisSemanticCacheHotStore(
            vectorStore, "embeddinggemma-v1", 2, Clock.systemUTC(), redisClient, "prefix:");
    SemanticCacheInvalidation invalidation =
        new SemanticCacheInvalidation(
            TENANT_ID,
            PRINCIPAL_ID,
            "CHAT_INFORMATIONAL",
            SemanticCacheDependencyChanged.Dependency.PRICE,
            "price-v2");

    hotStore.invalidate(invalidation);

    verify(redisClient).smembers("prefix:tenant:" + TENANT_ID + ":principal:" + PRINCIPAL_ID);
    verify(redisClient).del(documentKey);
    verify(redisClient).del("prefix:tenant:" + TENANT_ID + ":principal:" + PRINCIPAL_ID);
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        TENANT_ID,
        PRINCIPAL_ID,
        Set.of("CLIENT"),
        CONVERSATION_ID,
        WORKFLOW_ID,
        "trace-hot-cache",
        "idempotency-hot-cache");
  }

  private static String encodeTagValue(String value) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }
}
