package com.emme.studio.documents.entity;

import com.emme.shared.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "document")
public class Document extends TenantOwnedEntity {

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "source_type", nullable = false, length = 30)
  private String sourceType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private DocumentStatus status = DocumentStatus.UPLOADED;

  @Column(name = "version", nullable = false)
  private int version = 1;

  protected Document() {}

  public Document(UUID tenantId, String name, String sourceType) {
    super(tenantId);
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSourceType() {
    return sourceType;
  }

  public void setSourceType(String sourceType) {
    this.sourceType = sourceType;
  }

  public DocumentStatus getStatus() {
    return status;
  }

  public void setStatus(DocumentStatus status) {
    this.status = status;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  /** Transition to PROCESSING state. Only valid from UPLOADED */
  public void markProcessing() {
    if (status != DocumentStatus.UPLOADED) {
      throw new IllegalStateException("Cannot process document with status: " + status);
    }
    status = DocumentStatus.PROCESSING;
  }

  /** Transition to READY state. Only valid from PROCESSING */
  public void markReady() {
    if (status != DocumentStatus.PROCESSING) {
      throw new IllegalStateException("Cannot mark ready document with status: " + status);
    }
    status = DocumentStatus.READY;
  }

  /** Transition to FAILED state. Valid from UPLOADED or PROCESSING */
  public void markFailed() {
    if (status != DocumentStatus.UPLOADED && status != DocumentStatus.PROCESSING) {
      throw new IllegalStateException("Cannot fail document with status: " + status);
    }
    status = DocumentStatus.FAILED;
  }

  /** Transition to RETIRED state */
  public void markRetired() {
    if (status == DocumentStatus.RETIRED) {
      throw new IllegalStateException("Document is already retired");
    }
    status = DocumentStatus.RETIRED;
  }
}
