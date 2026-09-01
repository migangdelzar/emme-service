package com.emme.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.adapter.out.provider.springai.RedisSemanticCacheHotStore;
import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.semantic.EmbeddingVector;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.RedisClient;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Redis semantic projection integration")
class RedisSemanticIntegrationTest {

  private static final String IMAGE = "redis:8.10.1-alpine3.23";
  private static final String MODEL_VERSION = "ollama-embeddinggemma:300m";
  private static final String PREFIX = "integration:ai:semantic-cache:";
  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID OTHER_TENANT_ID = UUID.randomUUID();
  private static final UUID PRINCIPAL_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();
  private static final EmbeddingVector QUERY = new EmbeddingVector(MODEL_VERSION, vector());

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse(IMAGE)).withExposedPorts(6379);

  private RedisClient redisClient;
  private RedisSemanticCacheHotStore hotStore;

  @BeforeAll
  void connectToContainer() {
    redisClient = RedisClient.create(REDIS.getHost(), REDIS.getFirstMappedPort());
    EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
    when(embeddingModel.dimensions()).thenReturn(768);
    when(embeddingModel.embed(any(Document.class))).thenReturn(embedding());
    when(embeddingModel.embed(anyString())).thenReturn(embedding());
    when(embeddingModel.embed(anyList(), any(EmbeddingOptions.class), any(BatchingStrategy.class)))
        .thenReturn(List.of(embedding()));

    RedisVectorStore vectorStore =
        RedisVectorStore.builder(redisClient, embeddingModel)
            .indexName("integration-ai-semantic-cache")
            .prefix(PREFIX)
            .vectorAlgorithm(RedisVectorStore.Algorithm.FLAT)
            .initializeSchema(true)
            .metadataFields(
                RedisVectorStore.MetadataField.tag("tenantId"),
                RedisVectorStore.MetadataField.tag("principalId"),
                RedisVectorStore.MetadataField.tag("durableCacheId"),
                RedisVectorStore.MetadataField.tag("cacheKind"),
                RedisVectorStore.MetadataField.tag("contextFingerprint"),
                RedisVectorStore.MetadataField.tag("promptVersion"),
                RedisVectorStore.MetadataField.tag("embeddingModelName"),
                RedisVectorStore.MetadataField.tag("embeddingModelVersion"),
                RedisVectorStore.MetadataField.text("responsePayload"),
                RedisVectorStore.MetadataField.numeric("expiresAt"))
            .build();
    vectorStore.afterPropertiesSet();
    hotStore =
        new RedisSemanticCacheHotStore(
            vectorStore, MODEL_VERSION, 768, java.time.Clock.systemUTC(), redisClient, PREFIX);
  }

  @AfterAll
  void closeClient() {
    if (redisClient != null) {
      redisClient.close();
    }
  }

  @Test
  void writesReadsAndScopesAHotProjectionByAuthenticatedTenantAndPrincipal() {
    UUID durableId = UUID.randomUUID();
    SemanticCachePort.Put write =
        new SemanticCachePort.Put(
            "CHAT_INFORMATIONAL",
            "What are your hours?",
            "context-v1:empty",
            "chat-v1",
            "{\"text\":\"We are open.\"}",
            Instant.now().plusSeconds(60),
            QUERY,
            "integration-write-1");

    AiExecutionContext context = context(TENANT_ID);
    AiExecutionContextScope.run(context, () -> hotStore.put(durableId, write));

    assertThat(redisClient.jsonGet(PREFIX + "cache-" + durableId)).isNotNull();

    var matches =
        AiExecutionContextScope.call(
            context,
            () ->
                hotStore.find(
                    new SemanticCachePort.Lookup(
                        "CHAT_INFORMATIONAL", "context-v1:empty", "chat-v1", QUERY),
                    "What are your hours?",
                    2));

    assertThat(matches)
        .singleElement()
        .extracting(SemanticCachePort.Candidate::id)
        .isEqualTo(durableId);
    assertThat(redisClient.expireTime(PREFIX + "cache-" + durableId)).isPositive();

    assertThat(
            AiExecutionContextScope.call(
                context(OTHER_TENANT_ID),
                () ->
                    hotStore.find(
                        new SemanticCachePort.Lookup(
                            "CHAT_INFORMATIONAL", "context-v1:empty", "chat-v1", QUERY),
                        "What are your hours?",
                        2)))
        .isEmpty();
  }

  private static AiExecutionContext context(UUID tenantId) {
    return new AiExecutionContext(
        tenantId,
        PRINCIPAL_ID,
        Set.of("ROLE_CLIENT"),
        CONVERSATION_ID,
        WORKFLOW_ID,
        "trace-redis-integration",
        "idempotency-redis-integration");
  }

  private static List<Float> vector() {
    var values = new java.util.ArrayList<Float>(java.util.Collections.nCopies(768, 0.0f));
    values.set(0, 1.0f);
    return values;
  }

  private static float[] embedding() {
    float[] values = new float[768];
    values[0] = 1.0f;
    return values;
  }
}
