package com.emme.assistant.ai.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** JPA mapping for design-image metadata; binary content remains in external storage. */
@Entity
@Table(name = "ai_design_image")
public class DesignImageEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Column(name = "workflow_id", nullable = false, updatable = false)
  private UUID workflowId;

  @Column(name = "storage_key", nullable = false, length = 1000)
  private String storageKey;

  @Column(name = "media_type", nullable = false, length = 120)
  private String mediaType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected DesignImageEntity() {}

  public DesignImageEntity(
      UUID tenantId, UUID workflowId, String storageKey, String mediaType, long sizeBytes) {
    this.id = UUID.randomUUID();
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    this.workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
    this.storageKey = requireText(storageKey, "storageKey");
    this.mediaType = requireText(mediaType, "mediaType");
    if (sizeBytes <= 0) {
      throw new IllegalArgumentException("sizeBytes must be positive");
    }
    this.sizeBytes = sizeBytes;
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getWorkflowId() {
    return workflowId;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public String getMediaType() {
    return mediaType;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
