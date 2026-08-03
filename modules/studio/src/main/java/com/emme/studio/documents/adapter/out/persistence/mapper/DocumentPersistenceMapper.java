package com.emme.studio.documents.adapter.out.persistence.mapper;

import com.emme.studio.documents.adapter.out.persistence.entity.DocumentChunkEntity;
import com.emme.studio.documents.adapter.out.persistence.entity.DocumentEntity;
import com.emme.studio.documents.domain.model.Document;
import com.emme.studio.documents.domain.model.DocumentChunk;

/** Maps document domain models to persistence representations and back. */
public final class DocumentPersistenceMapper {

  public DocumentEntity toEntity(Document document) {
    return DocumentEntity.from(document);
  }

  public Document toDomain(DocumentEntity entity) {
    return entity.toDomain();
  }

  public DocumentChunkEntity toEntity(DocumentChunk chunk) {
    return DocumentChunkEntity.from(chunk);
  }

  public DocumentChunk toDomain(DocumentChunkEntity entity) {
    return entity.toDomain();
  }
}
