package com.emme.assistant.ai.configuration;

import com.emme.assistant.ai.adapter.out.provider.springai.RedisSemanticCacheHotStore;
import com.emme.assistant.ai.application.port.out.SemanticCacheHotStore;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.toolsearch.ToolIndex;
import org.springframework.ai.tool.toolsearch.index.vectorstore.VectorToolIndex;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.RedisClient;

/** Opt-in composition root for Spring AI's Redis Stack vector-store projection. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RedisSemanticProperties.class)
@ConditionalOnProperty(prefix = "app.ai.redis-semantic", name = "enabled", havingValue = "true")
@ConditionalOnBean(EmbeddingModel.class)
public class SpringAiRedisSemanticConfiguration {

  @Bean(name = "aiRedisSemanticClient", destroyMethod = "close")
  @ConditionalOnMissingBean(name = "aiRedisSemanticClient")
  RedisClient redisSemanticClient(RedisSemanticProperties properties) {
    return RedisClient.create(properties.host(), properties.port());
  }

  @Bean(name = "aiRedisSemanticVectorStore")
  @ConditionalOnMissingBean(name = "aiRedisSemanticVectorStore")
  RedisVectorStore redisSemanticVectorStore(
      RedisClient redisClient,
      EmbeddingModel embeddingModel,
      RedisSemanticProperties properties,
      AiProperties aiProperties) {
    requireEmbeddingContract(aiProperties, properties);
    return RedisVectorStore.builder(redisClient, embeddingModel)
        .indexName(properties.indexName())
        .prefix(properties.prefix())
        .initializeSchema(properties.initializeSchema())
        .metadataFields(
            RedisVectorStore.MetadataField.tag("tenantId"),
            RedisVectorStore.MetadataField.tag("principalId"),
            RedisVectorStore.MetadataField.tag("durableCacheId"),
            RedisVectorStore.MetadataField.tag("cacheKind"),
            RedisVectorStore.MetadataField.tag("contextFingerprint"),
            RedisVectorStore.MetadataField.tag("promptVersion"),
            RedisVectorStore.MetadataField.tag("embeddingModelVersion"),
            RedisVectorStore.MetadataField.text("responsePayload"),
            RedisVectorStore.MetadataField.numeric("expiresAt"))
        .build();
  }

  @Bean
  @ConditionalOnMissingBean(SemanticCacheHotStore.class)
  SemanticCacheHotStore semanticCacheHotStore(
      @Qualifier("aiRedisSemanticVectorStore") RedisVectorStore vectorStore,
      @Qualifier("aiRedisSemanticClient") RedisClient redisClient,
      RedisSemanticProperties properties,
      AiProperties aiProperties) {
    requireEmbeddingContract(aiProperties, properties);
    return new RedisSemanticCacheHotStore(
        vectorStore,
        properties.embeddingModelVersion(),
        properties.embeddingDimension(),
        java.time.Clock.systemUTC(),
        redisClient,
        properties.prefix());
  }

  @Bean(name = "aiRedisToolVectorStore")
  @ConditionalOnMissingBean(name = "aiRedisToolVectorStore")
  @ConditionalOnProperty(
      prefix = "app.ai.redis-semantic",
      name = "tool-search-enabled",
      havingValue = "true")
  RedisVectorStore redisToolVectorStore(
      RedisClient redisClient,
      EmbeddingModel embeddingModel,
      RedisSemanticProperties properties,
      AiProperties aiProperties) {
    requireEmbeddingContract(aiProperties, properties);
    return RedisVectorStore.builder(redisClient, embeddingModel)
        .indexName(properties.indexName() + "-tools")
        .prefix(properties.prefix() + "tools:")
        .initializeSchema(properties.initializeSchema())
        .metadataFields(
            RedisVectorStore.MetadataField.tag("sessionId"),
            RedisVectorStore.MetadataField.tag("toolName"),
            RedisVectorStore.MetadataField.tag("toolDescription"))
        .build();
  }

  @Bean(name = "aiRedisToolIndex")
  @ConditionalOnMissingBean(name = "aiRedisToolIndex")
  @ConditionalOnProperty(
      prefix = "app.ai.redis-semantic",
      name = "tool-search-enabled",
      havingValue = "true")
  ToolIndex redisToolIndex(@Qualifier("aiRedisToolVectorStore") RedisVectorStore vectorStore) {
    return new VectorToolIndex(vectorStore);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(
      prefix = "app.ai.redis-semantic",
      name = "tool-search-enabled",
      havingValue = "true")
  ToolSearchToolCallingAdvisor toolSearchToolCallingAdvisor(
      @Qualifier("aiRedisToolIndex") ToolIndex toolIndex, RedisSemanticProperties properties) {
    return ToolSearchToolCallingAdvisor.builder()
        .toolIndex(toolIndex)
        .maxResults(properties.toolSearchMaxResults())
        .sessionIdKeyName(ChatMemory.CONVERSATION_ID)
        .build();
  }

  private static void requireEmbeddingContract(
      AiProperties aiProperties, RedisSemanticProperties redisProperties) {
    if (aiProperties.embeddingDimension() != redisProperties.embeddingDimension()
        || !aiProperties.embeddingModelVersion().equals(redisProperties.embeddingModelVersion())) {
      throw new IllegalArgumentException(
          "Redis semantic embedding settings must match configured embedding settings");
    }
  }
}
