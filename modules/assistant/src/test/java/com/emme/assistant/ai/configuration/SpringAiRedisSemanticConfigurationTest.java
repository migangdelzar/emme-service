package com.emme.assistant.ai.configuration;

import static com.emme.assistant.ai.EmbeddingTestVectors.testEmbedding;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.embedding.EmbeddingService;
import com.emme.ai.platform.configuration.AiProviderProperties;
import com.emme.assistant.ai.application.port.out.EmbeddingProviderUnavailableException;
import com.emme.assistant.ai.application.provider.EmbeddingModelSelector;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.tool.toolsearch.ToolIndex;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import redis.clients.jedis.RedisClient;

class SpringAiRedisSemanticConfigurationTest {

  @Test
  void buildsTheOfficialSpringAiRedisVectorStoreWithTenantMetadataFields() {
    SpringAiRedisSemanticConfiguration configuration = new SpringAiRedisSemanticConfiguration();
    RedisVectorStore vectorStore =
        configuration.redisSemanticVectorStore(
            mock(RedisClient.class),
            mock(EmbeddingService.class),
            redisProperties(),
            aiProperties());

    assertThat(vectorStore).isNotNull();
  }

  @Test
  void buildsTheOfficialSpringAiToolSearchAdvisorOverASeparateRedisIndex() {
    SpringAiRedisSemanticConfiguration configuration = new SpringAiRedisSemanticConfiguration();
    RedisVectorStore vectorStore =
        configuration.redisToolVectorStore(
            mock(RedisClient.class),
            mock(EmbeddingService.class),
            redisProperties(),
            aiProperties());
    ToolIndex toolIndex = configuration.redisToolIndex(vectorStore);

    ToolSearchToolCallingAdvisor advisor =
        configuration.toolSearchToolCallingAdvisor(toolIndex, redisProperties());

    assertThat(advisor).isNotNull();
    assertThat(advisor.getName()).isEqualTo("ToolSearchToolCallingAdvisor");
  }

  @Test
  void rejectsRedisSemanticSettingsThatUseADifferentEmbeddingSpace() {
    SpringAiRedisSemanticConfiguration configuration = new SpringAiRedisSemanticConfiguration();
    RedisSemanticProperties redisProperties =
        new RedisSemanticProperties(
            true, "localhost", 6379, "index", "prefix", "other-model", false, null);

    assertThatThrownBy(
            () ->
                configuration.redisSemanticVectorStore(
                    mock(RedisClient.class),
                    mock(EmbeddingService.class),
                    redisProperties,
                    aiProperties()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Redis semantic embedding settings must match configured embedding settings");
  }

  @Test
  void rejectsRedisHotStoreSettingsThatUseADifferentEmbeddingSpace() {
    SpringAiRedisSemanticConfiguration configuration = new SpringAiRedisSemanticConfiguration();
    RedisSemanticProperties redisProperties =
        new RedisSemanticProperties(
            true, "localhost", 6379, "index", "prefix", "other-model", false, null);

    assertThatThrownBy(
            () ->
                configuration.semanticCacheHotStore(
                    mock(RedisVectorStore.class),
                    mock(RedisClient.class),
                    redisProperties,
                    aiProperties()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Redis semantic embedding settings must match configured embedding settings");
  }

  @Test
  void routesRedisVectorStoreEmbeddingsThroughTheConfiguredModelSelector() {
    EmbeddingService primary = mock(EmbeddingService.class);
    EmbeddingService fallback = mock(EmbeddingService.class);
    when(primary.embed("faq"))
        .thenThrow(new EmbeddingProviderUnavailableException("primary unavailable"));
    when(fallback.embed("faq"))
        .thenReturn(testEmbedding("ollama-embeddinggemma:300m", List.of(0.2f, 0.8f)));
    EmbeddingService selector =
        new EmbeddingModelSelector(
            List.of(
                new EmbeddingModelSelector.Provider("primary", primary),
                new EmbeddingModelSelector.Provider("fallback", fallback)));
    SpringAiRedisSemanticConfiguration configuration = new SpringAiRedisSemanticConfiguration();

    var redisEmbeddingModel = configuration.redisEmbeddingModel(selector, aiProperties(2));

    assertThat(redisEmbeddingModel.embed("faq")).containsExactly(0.2f, 0.8f);
    org.mockito.Mockito.verify(primary).embed("faq");
    org.mockito.Mockito.verify(fallback).embed("faq");
    var invocationOrder = inOrder(primary, fallback);
    invocationOrder.verify(primary).embed("faq");
    invocationOrder.verify(fallback).embed("faq");
  }

  private static RedisSemanticProperties redisProperties() {
    return new RedisSemanticProperties(
        true, "localhost", 6379, "index", "prefix", "ollama-embeddinggemma:300m", false, null);
  }

  private static AiProviderProperties aiProperties() {
    return new AiProviderProperties(null, null, null, false);
  }

  private static AiProviderProperties aiProperties(int dimension) {
    return new AiProviderProperties(
        "mock",
        null,
        new AiProviderProperties.EmbeddingConfig(
            "embeddinggemma:300m", "http://localhost:11434", null, dimension),
        true);
  }
}
