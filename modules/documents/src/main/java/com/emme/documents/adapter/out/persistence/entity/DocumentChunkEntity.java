package com.emme.documents.adapter.out.persistence.entity;

import com.emme.documents.domain.model.DocumentChunk;
import com.emme.shared.persistence.TenantOwnedEntity;
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
public class DocumentChunkEntity extends TenantOwnedEntity {

  @Column(name = "document_id", nullable = false)
  private UUID documentId;

  @Column(name = "chunk_index", nullable = false)
  private int chunkIndex;

  @Column(name = "content", nullable = false, length = 2000)
  private String content;

  @Column(name = "content_fingerprint", nullable = false, length = 128)
  private String contentFingerprint;

  protected DocumentChunkEntity() {}

  public DocumentChunkEntity(
      UUID tenantId, UUID documentId, int chunkIndex, String content, String contentFingerprint) {
    super(tenantId);
    this.documentId = Objects.requireNonNull(documentId, "documentId must not be null");
    this.chunkIndex = chunkIndex;
    this.content = Objects.requireNonNull(content, "content must not be null");
    this.contentFingerprint =
        Objects.requireNonNull(contentFingerprint, "contentFingerprint must not be null");
  }

  private DocumentChunkEntity(DocumentChunk chunk) {
    super(chunk.tenantId());
    setId(chunk.id());
    this.documentId = chunk.documentId();
    this.chunkIndex = chunk.chunkIndex();
    this.content = chunk.content();
    this.contentFingerprint = chunk.contentFingerprint();
  }

  public static DocumentChunkEntity from(DocumentChunk chunk) {
    return new DocumentChunkEntity(chunk);
  }

  public DocumentChunk toDomain() {
    return new DocumentChunk(
        getId(), getTenantId(), documentId, chunkIndex, content, contentFingerprint);
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
