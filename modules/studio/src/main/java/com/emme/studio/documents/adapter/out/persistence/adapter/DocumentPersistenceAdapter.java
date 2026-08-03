package com.emme.studio.documents.adapter.out.persistence.adapter;

import com.emme.studio.documents.adapter.out.persistence.entity.DocumentEntity;
import com.emme.studio.documents.adapter.out.persistence.mapper.DocumentPersistenceMapper;
import com.emme.studio.documents.adapter.out.persistence.repository.SpringDataDocumentChunkRepository;
import com.emme.studio.documents.adapter.out.persistence.repository.SpringDataDocumentRepository;
import com.emme.studio.documents.application.port.out.DocumentRepository;
import com.emme.studio.documents.domain.model.Document;
import com.emme.studio.documents.domain.model.DocumentChunk;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** JPA adapter implementing the application-owned document persistence port. */
@Component
public class DocumentPersistenceAdapter implements DocumentRepository {

  private final SpringDataDocumentRepository documents;
  private final SpringDataDocumentChunkRepository chunks;
  private final DocumentPersistenceMapper mapper;

  public DocumentPersistenceAdapter(
      SpringDataDocumentRepository documents,
      SpringDataDocumentChunkRepository chunks,
      DocumentPersistenceMapper mapper) {
    this.documents = documents;
    this.chunks = chunks;
    this.mapper = mapper;
  }

  @Override
  public Optional<Document> findByTenantIdAndId(UUID tenantId, UUID documentId) {
    return documents.findByTenantIdAndId(tenantId, documentId).map(mapper::toDomain);
  }

  @Override
  public List<Document> findByTenantId(UUID tenantId) {
    return documents.findByTenantId(tenantId).stream()
        .map(entity -> mapper.toDomain(entity))
        .toList();
  }

  @Override
  public Document save(Document document) {
    DocumentEntity entity =
        documents
            .findByTenantIdAndId(document.tenantId(), document.id())
            .orElseGet(() -> mapper.toEntity(document));
    entity.setName(document.name());
    entity.setSourceType(document.sourceType());
    entity.setStatus(
        com.emme.studio.documents.adapter.out.persistence.entity.DocumentStatus.valueOf(
            document.status().name()));
    entity.setVersion(document.version());
    return mapper.toDomain(documents.save(entity));
  }

  @Override
  public List<DocumentChunk> findChunks(UUID tenantId, UUID documentId) {
    return chunks.findByTenantIdAndDocumentIdOrderByChunkIndexAsc(tenantId, documentId).stream()
        .map(entity -> mapper.toDomain(entity))
        .toList();
  }

  @Override
  public List<DocumentChunk> findChunksByTenantIdAndIds(UUID tenantId, List<UUID> chunkIds) {
    return chunks.findByTenantIdAndIdIn(tenantId, chunkIds).stream().map(mapper::toDomain).toList();
  }

  @Override
  public void replaceChunks(UUID tenantId, UUID documentId, List<DocumentChunk> newChunks) {
    chunks.deleteByTenantIdAndDocumentId(tenantId, documentId);
    chunks.flush();
    chunks.saveAll(newChunks.stream().map(mapper::toEntity).toList());
  }
}
