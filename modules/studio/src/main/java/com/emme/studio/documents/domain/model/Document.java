package com.emme.studio.documents.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Framework-free document aggregate owning its lifecycle invariants. */
public final class Document {

  private final UUID id;
  private final UUID tenantId;
  private final Instant createdAt;
  private final String name;
  private final String sourceType;
  private DocumentStatus status;
  private int version;

  public Document(UUID tenantId, String name, String sourceType) {
    this(UUID.randomUUID(), tenantId, name, sourceType, DocumentStatus.UPLOADED, 1, Instant.now());
  }

  private Document(
      UUID id,
      UUID tenantId,
      String name,
      String sourceType,
      DocumentStatus status,
      int version,
      Instant createdAt) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
    this.status = Objects.requireNonNull(status, "status must not be null");
    this.version = version;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
  }

  public static Document rehydrate(
      UUID id,
      UUID tenantId,
      String name,
      String sourceType,
      DocumentStatus status,
      int version,
      Instant createdAt) {
    return new Document(id, tenantId, name, sourceType, status, version, createdAt);
  }

  public UUID id() {
    return id;
  }

  public UUID tenantId() {
    return tenantId;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public String name() {
    return name;
  }

  public String sourceType() {
    return sourceType;
  }

  public DocumentStatus status() {
    return status;
  }

  public int version() {
    return version;
  }

  public void markProcessing() {
    requireStatus(DocumentStatus.UPLOADED, "process");
    status = DocumentStatus.PROCESSING;
  }

  public void markReady() {
    requireStatus(DocumentStatus.PROCESSING, "mark ready");
    status = DocumentStatus.READY;
  }

  public void markFailed() {
    if (status != DocumentStatus.UPLOADED && status != DocumentStatus.PROCESSING) {
      throw new IllegalStateException("Cannot fail document with status: " + status);
    }
    status = DocumentStatus.FAILED;
  }

  public void markRetired() {
    if (status == DocumentStatus.RETIRED) {
      throw new IllegalStateException("Document is already retired");
    }
    status = DocumentStatus.RETIRED;
  }

  private void requireStatus(DocumentStatus expected, String action) {
    if (status != expected) {
      throw new IllegalStateException("Cannot " + action + " document with status: " + status);
    }
  }
}
