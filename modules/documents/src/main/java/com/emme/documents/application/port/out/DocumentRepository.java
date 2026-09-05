package com.emme.documents.application.port.out;

import com.emme.documents.domain.model.Document;
import com.emme.documents.domain.model.DocumentChunk;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence capability required by document use cases. */
public interface DocumentRepository {

  Optional<Document> findById(UUID documentId);

  List<Document> findAll();

  Document save(Document document);

  List<DocumentChunk> findChunks(UUID documentId);

  List<DocumentChunk> findChunksByIds(List<UUID> chunkIds);

  void replaceChunks(UUID documentId, List<DocumentChunk> chunks);
}
