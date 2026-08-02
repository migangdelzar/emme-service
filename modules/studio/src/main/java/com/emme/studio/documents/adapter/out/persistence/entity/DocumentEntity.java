package com.emme.studio.documents.adapter.out.persistence.entity;

import com.emme.shared.TenantOwnedEntity;
import com.emme.studio.documents.domain.model.Document;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "document")
public class DocumentEntity extends TenantOwnedEntity {

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "source_type", nullable = false, length = 30)
  private String sourceType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private DocumentStatus status = DocumentStatus.UPLOADED;

  @Column(name = "version", nullable = false)
  private int version = 1;

  protected DocumentEntity() {}

  public DocumentEntity(UUID tenantId, String name, String sourceType) {
    super(tenantId);
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
  }

  private DocumentEntity(Document document) {
    super(document.tenantId());
    setId(document.id());
    this.name = document.name();
    this.sourceType = document.sourceType();
    this.status = DocumentStatus.valueOf(document.status().name());
    this.version = document.version();
  }

  public static DocumentEntity from(Document document) {
    return new DocumentEntity(document);
  }

  public Document toDomain() {
    return Document.rehydrate(
        getId(),
        getTenantId(),
        name,
        sourceType,
        com.emme.studio.documents.domain.model.DocumentStatus.valueOf(status.name()),
        version,
        getCreatedAt());
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
}
