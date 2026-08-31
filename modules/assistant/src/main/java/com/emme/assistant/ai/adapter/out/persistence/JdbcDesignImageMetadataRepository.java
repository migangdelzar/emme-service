package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.assistant.ai.application.port.out.DesignImageMetadataRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public final class JdbcDesignImageMetadataRepository implements DesignImageMetadataRepository {
  private final JdbcClient jdbc;

  public JdbcDesignImageMetadataRepository(JdbcClient jdbc) {
    this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
  }

  @Override
  public void save(
      UUID tenantId, UUID workflowId, String storageKey, String mediaType, long sizeBytes) {
    jdbc.sql(
            """
            INSERT INTO ai_design_image (tenant_id, workflow_id, storage_key, media_type, size_bytes)
        VALUES (:tenantId, :workflowId, :storageKey, :mediaType, :sizeBytes)
        """)
        .param("tenantId", tenantId)
        .param("workflowId", workflowId)
        .param("storageKey", storageKey)
        .param("mediaType", mediaType)
        .param("sizeBytes", sizeBytes)
        .update();
  }

  @Override
  public void delete(UUID tenantId, UUID workflowId, String storageKey) {
    jdbc.sql(
            "DELETE FROM ai_design_image WHERE tenant_id = :tenantId AND workflow_id = :workflowId AND storage_key = :storageKey")
        .param("tenantId", tenantId)
        .param("workflowId", workflowId)
        .param("storageKey", storageKey)
        .update();
  }
}
