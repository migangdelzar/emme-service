package com.emme.assistant.ai.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Optional Redis Stack settings for the non-authoritative semantic cache projection. */
@ConfigurationProperties("app.ai.redis-semantic")
public record RedisSemanticProperties(
    boolean enabled,
    String host,
    Integer port,
    String indexName,
    String prefix,
    String embeddingModelVersion,
    Integer embeddingDimension,
    boolean initializeSchema,
    Integer toolSearchMaxResults) {

  public RedisSemanticProperties {
    host = host == null ? "localhost" : host;
    port = port == null ? 6379 : port;
    indexName = indexName == null ? "emme-ai-semantic-cache" : indexName;
    prefix = prefix == null ? "emme:ai:semantic-cache:" : prefix;
    embeddingModelVersion =
        embeddingModelVersion == null ? "ollama-embeddinggemma:300m" : embeddingModelVersion;
    embeddingDimension = embeddingDimension == null ? 768 : embeddingDimension;
    toolSearchMaxResults = toolSearchMaxResults == null ? 5 : toolSearchMaxResults;
    requireText(host, "host");
    requireText(indexName, "indexName");
    requireText(prefix, "prefix");
    requireText(embeddingModelVersion, "embeddingModelVersion");
    if (port < 1 || port > 65535) {
      throw new IllegalArgumentException("port must be between 1 and 65535");
    }
    if (embeddingDimension <= 0) {
      throw new IllegalArgumentException("embeddingDimension must be positive");
    }
    if (toolSearchMaxResults <= 0) {
      throw new IllegalArgumentException("toolSearchMaxResults must be positive");
    }
  }

  private static void requireText(String value, String field) {
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
