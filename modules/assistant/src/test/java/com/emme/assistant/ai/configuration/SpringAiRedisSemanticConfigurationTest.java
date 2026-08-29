package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
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
            mock(RedisClient.class),
            mock(EmbeddingModel.class),
            new RedisSemanticProperties(
                true, "localhost", 6379, "index", "prefix", "model", 768, false));

    assertThat(vectorStore).isNotNull();
  }

  @Test
  void buildsTheOfficialSpringAiToolSearchAdvisorOverASeparateRedisIndex() {
    SpringAiRedisSemanticConfiguration configuration = new SpringAiRedisSemanticConfiguration();
    RedisVectorStore vectorStore =
        configuration.redisToolVectorStore(
            mock(RedisClient.class),
            mock(EmbeddingModel.class),
            new RedisSemanticProperties(
                true, "localhost", 6379, "index", "prefix", "model", 768, false));
    ToolIndex toolIndex = configuration.redisToolIndex(vectorStore);

    ToolSearchToolCallingAdvisor advisor =
        configuration.toolSearchToolCallingAdvisor(
            toolIndex,
            new RedisSemanticProperties(
                true, "localhost", 6379, "index", "prefix", "model", 768, false));

    assertThat(advisor).isNotNull();
    assertThat(advisor.getName()).isEqualTo("ToolSearchToolCallingAdvisor");
  }
}
