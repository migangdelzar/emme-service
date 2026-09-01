package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.toolsearch.ToolIndex;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import redis.clients.jedis.RedisClient;

class SpringAiRedisSemanticConfigurationTest {

  @Test
  void buildsTheOfficialSpringAiRedisVectorStoreWithTenantMetadataFields() {
    SpringAiRedisSemanticConfiguration configuration = new SpringAiRedisSemanticConfiguration();
    RedisVectorStore vectorStore =
        configuration.redisSemanticVectorStore(
            mock(RedisClient.class), mock(EmbeddingModel.class), redisProperties(), aiProperties());

    assertThat(vectorStore).isNotNull();
  }

  @Test
  void buildsTheOfficialSpringAiToolSearchAdvisorOverASeparateRedisIndex() {
    SpringAiRedisSemanticConfiguration configuration = new SpringAiRedisSemanticConfiguration();
    RedisVectorStore vectorStore =
        configuration.redisToolVectorStore(
            mock(RedisClient.class), mock(EmbeddingModel.class), redisProperties(), aiProperties());
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
            true, "localhost", 6379, "index", "prefix", "other-model", 1024, false, null);

    assertThatThrownBy(
            () ->
                configuration.redisSemanticVectorStore(
                    mock(RedisClient.class),
                    mock(EmbeddingModel.class),
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
            true, "localhost", 6379, "index", "prefix", "other-model", 1024, false, null);

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

  private static RedisSemanticProperties redisProperties() {
    return new RedisSemanticProperties(
        true, "localhost", 6379, "index", "prefix", "ollama-embeddinggemma:300m", 768, false, null);
  }

  private static AiProperties aiProperties() {
    return new AiProperties(null, null, null, false);
  }
}
