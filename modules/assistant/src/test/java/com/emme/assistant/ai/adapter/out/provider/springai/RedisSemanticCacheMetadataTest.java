package com.emme.assistant.ai.adapter.out.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RedisSemanticCacheMetadataTest {

  @Test
  void definesOneMetadataContractForTheRedisSemanticCacheProjection() {
    assertThat(RedisSemanticCacheMetadata.keys())
        .containsExactly(
            "tenantId",
            "principalId",
            "durableCacheId",
            "cacheKind",
            "contextFingerprint",
            "promptVersion",
            "embeddingModelName",
            "embeddingModelVersion",
            "responseProvider",
            "responseModel",
            "knowledgeVersion",
            "policyVersion",
            "sourceVersion",
            "responseChannel",
            "responseLocale",
            "responseQuoteTemplateVersion",
            "responsePayload",
            "expiresAt");
  }

  @Test
  void returnsAnImmutableMetadataKeyList() {
    List<String> keys = RedisSemanticCacheMetadata.keys();

    assertThat(keys).isUnmodifiable();
  }
}
