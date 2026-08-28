package com.emme.assistant.ai.adapter.out.redis;

import java.util.UUID;

/** Canonical tenant-scoped Redis key names for temporary AI state. */
final class RedisAiKeys {

  private RedisAiKeys() {
    throw new UnsupportedOperationException("Utility class");
  }

  static String workflowStateKey(UUID tenantId, UUID workflowId) {
    return "ai:workflow:%s:%s:status".formatted(require(tenantId), require(workflowId));
  }

  static String conversationLockKey(UUID tenantId, UUID conversationId) {
    return "ai:lock:%s:%s".formatted(require(tenantId), require(conversationId));
  }

  static String liveEventStreamKey(UUID tenantId, UUID conversationId) {
    return "ai:stream:%s:%s:events".formatted(require(tenantId), require(conversationId));
  }

  static String sessionKey(UUID tenantId, UUID principalId) {
    return "ai:session:%s:%s".formatted(require(tenantId), require(principalId));
  }

  private static UUID require(UUID value) {
    if (value == null) {
      throw new NullPointerException("Redis key identity must not be null");
    }
    return value;
  }
}
