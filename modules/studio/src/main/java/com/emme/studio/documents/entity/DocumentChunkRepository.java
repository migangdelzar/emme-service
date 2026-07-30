package com.emme.studio.documents.entity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

  List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);

  void deleteByDocumentId(UUID documentId);
}
