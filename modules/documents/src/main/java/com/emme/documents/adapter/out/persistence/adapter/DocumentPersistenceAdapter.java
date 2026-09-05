package com.emme.documents.adapter.out.persistence.adapter;

import com.emme.documents.adapter.out.persistence.entity.DocumentChunkEntity;
import com.emme.documents.adapter.out.persistence.entity.DocumentEntity;
import com.emme.documents.adapter.out.persistence.repository.SpringDataDocumentChunkRepository;
import com.emme.documents.adapter.out.persistence.repository.SpringDataDocumentRepository;
import com.emme.documents.application.port.out.DocumentRepository;
import com.emme.documents.domain.model.Document;
import com.emme.documents.domain.model.DocumentChunk;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** JPA adapter implementing the application-owned document persistence port. */
@Component
public class DocumentPersistenceAdapter implements DocumentRepository {

  private final SpringDataDocumentRepository documents;
  private final SpringDataDocumentChunkRepository chunks;

  public DocumentPersistenceAdapter(
      SpringDataDocumentRepository documents, SpringDataDocumentChunkRepository chunks) {
    this.documents = documents;
    this.chunks = chunks;
  }

  @Override
  public Optional<Document> findById(UUID documentId) {
    return documents.findById(documentId).map(DocumentEntity::toDomain);
  }

  @Override
  public List<Document> findAll() {
    return documents.findAll().stream().map(DocumentEntity::toDomain).toList();
  }

  @Override
  public Document save(Document document) {
    DocumentEntity entity =
        documents.findById(document.id()).orElseGet(() -> DocumentEntity.from(document));
    entity.setName(document.name());
    entity.setSourceType(document.sourceType());
    entity.setStatus(
        com.emme.documents.adapter.out.persistence.entity.DocumentStatus.valueOf(
            document.status().name()));
    entity.setVersion(document.version());
    return documents.save(entity).toDomain();
  }

  @Override
  public List<DocumentChunk> findChunks(UUID documentId) {
    return chunks.findByDocumentIdOrderByChunkIndexAsc(documentId).stream()
        .map(DocumentChunkEntity::toDomain)
        .toList();
  }

  @Override
  public List<DocumentChunk> findChunksByIds(List<UUID> chunkIds) {
    return chunks.findByIdIn(chunkIds).stream().map(DocumentChunkEntity::toDomain).toList();
  }

  @Override
  public void replaceChunks(UUID documentId, List<DocumentChunk> newChunks) {
    chunks.deleteByDocumentId(documentId);
    chunks.flush();
    chunks.saveAll(newChunks.stream().map(DocumentChunkEntity::from).toList());
  }
}
