package com.emme.studio.documents.application.port.out;

import com.emme.studio.documents.domain.model.Document;
import com.emme.studio.documents.domain.model.DocumentChunk;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence capability required by document use cases. */
public interface DocumentRepository {

  Optional<Document> findById(UUID documentId);

  Optional<Document> findByTenantIdAndId(UUID tenantId, UUID documentId);

  List<Document> findByTenantId(UUID tenantId);

  Document save(Document document);

  List<DocumentChunk> findChunks(UUID tenantId, UUID documentId);

  void replaceChunks(UUID tenantId, UUID documentId, List<DocumentChunk> chunks);
}
