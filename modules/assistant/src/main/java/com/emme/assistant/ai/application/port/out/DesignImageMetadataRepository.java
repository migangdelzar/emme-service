package com.emme.assistant.ai.application.port.out;

import java.util.UUID;

/** Durable metadata boundary; binary content remains in image storage. */
public interface DesignImageMetadataRepository {
  void save(UUID tenantId, UUID workflowId, String storageKey, String mediaType, long sizeBytes);

  void delete(UUID tenantId, UUID workflowId, String storageKey);
}
