package com.emme.studio.documents.entity;

import com.emme.shared.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "document_chunk",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"document_id", "chunk_index"})})
public class DocumentChunk extends TenantOwnedEntity {

  @Column(name = "document_id", nullable = false)
  private UUID documentId;

  @Column(name = "chunk_index", nullable = false)
  private int chunkIndex;

  @Column(name = "content", nullable = false, length = 2000)
  private String content;

  @Column(name = "content_fingerprint", nullable = false, length = 128)
  private String contentFingerprint;

  protected DocumentChunk() {}

  public DocumentChunk(
      UUID tenantId, UUID documentId, int chunkIndex, String content, String contentFingerprint) {
    super(tenantId);
    this.documentId = Objects.requireNonNull(documentId, "documentId must not be null");
    this.chunkIndex = chunkIndex;
    this.content = Objects.requireNonNull(content, "content must not be null");
    this.contentFingerprint =
        Objects.requireNonNull(contentFingerprint, "contentFingerprint must not be null");
  }

  public UUID getDocumentId() {
    return documentId;
  }

  public int getChunkIndex() {
    return chunkIndex;
  }

  public void setChunkIndex(int chunkIndex) {
    this.chunkIndex = chunkIndex;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getContentFingerprint() {
    return contentFingerprint;
  }

  public void setContentFingerprint(String contentFingerprint) {
    this.contentFingerprint = contentFingerprint;
  }
}
