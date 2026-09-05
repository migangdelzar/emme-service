package com.emme.assistant.ai.adapter.out.provider.springai;

import java.util.List;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;

/** Metadata contract shared by the Redis semantic-cache index and its hot-store adapter. */
public final class RedisSemanticCacheMetadata {

  public static final String TENANT_ID = "tenantId";
  public static final String PRINCIPAL_ID = "principalId";
  public static final String DURABLE_CACHE_ID = "durableCacheId";
  public static final String CACHE_KIND = "cacheKind";
  public static final String CONTEXT_FINGERPRINT = "contextFingerprint";
  public static final String PROMPT_VERSION = "promptVersion";
  public static final String EMBEDDING_MODEL_NAME = "embeddingModelName";
  public static final String EMBEDDING_MODEL_VERSION = "embeddingModelVersion";
  public static final String RESPONSE_PROVIDER = "responseProvider";
  public static final String RESPONSE_MODEL = "responseModel";
  public static final String KNOWLEDGE_VERSION = "knowledgeVersion";
  public static final String POLICY_VERSION = "policyVersion";
  public static final String SOURCE_VERSION = "sourceVersion";
  public static final String RESPONSE_CHANNEL = "responseChannel";
  public static final String RESPONSE_LOCALE = "responseLocale";
  public static final String RESPONSE_QUOTE_TEMPLATE_VERSION = "responseQuoteTemplateVersion";
  public static final String RESPONSE_PAYLOAD = "responsePayload";
  public static final String EXPIRES_AT = "expiresAt";

  private static final List<String> KEYS =
      List.of(
          TENANT_ID,
          PRINCIPAL_ID,
          DURABLE_CACHE_ID,
          CACHE_KIND,
          CONTEXT_FINGERPRINT,
          PROMPT_VERSION,
          EMBEDDING_MODEL_NAME,
          EMBEDDING_MODEL_VERSION,
          RESPONSE_PROVIDER,
          RESPONSE_MODEL,
          KNOWLEDGE_VERSION,
          POLICY_VERSION,
          SOURCE_VERSION,
          RESPONSE_CHANNEL,
          RESPONSE_LOCALE,
          RESPONSE_QUOTE_TEMPLATE_VERSION,
          RESPONSE_PAYLOAD,
          EXPIRES_AT);

  private RedisSemanticCacheMetadata() {}

  public static List<String> keys() {
    return KEYS;
  }

  public static RedisVectorStore.MetadataField[] vectorStoreFields() {
    return new RedisVectorStore.MetadataField[] {
      RedisVectorStore.MetadataField.tag(TENANT_ID),
      RedisVectorStore.MetadataField.tag(PRINCIPAL_ID),
      RedisVectorStore.MetadataField.tag(DURABLE_CACHE_ID),
      RedisVectorStore.MetadataField.tag(CACHE_KIND),
      RedisVectorStore.MetadataField.tag(CONTEXT_FINGERPRINT),
      RedisVectorStore.MetadataField.tag(PROMPT_VERSION),
      RedisVectorStore.MetadataField.tag(EMBEDDING_MODEL_NAME),
      RedisVectorStore.MetadataField.tag(EMBEDDING_MODEL_VERSION),
      RedisVectorStore.MetadataField.tag(RESPONSE_PROVIDER),
      RedisVectorStore.MetadataField.tag(RESPONSE_MODEL),
      RedisVectorStore.MetadataField.tag(KNOWLEDGE_VERSION),
      RedisVectorStore.MetadataField.tag(POLICY_VERSION),
      RedisVectorStore.MetadataField.tag(SOURCE_VERSION),
      RedisVectorStore.MetadataField.tag(RESPONSE_CHANNEL),
      RedisVectorStore.MetadataField.tag(RESPONSE_LOCALE),
      RedisVectorStore.MetadataField.tag(RESPONSE_QUOTE_TEMPLATE_VERSION),
      RedisVectorStore.MetadataField.text(RESPONSE_PAYLOAD),
      RedisVectorStore.MetadataField.numeric(EXPIRES_AT)
    };
  }
}
