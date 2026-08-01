package com.emme.studio.documents.adapter.out.persistence.repository;

import com.emme.studio.documents.adapter.out.persistence.entity.DocumentChunkEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataDocumentChunkRepository
    extends JpaRepository<DocumentChunkEntity, UUID> {

  List<DocumentChunkEntity> findByTenantIdAndDocumentIdOrderByChunkIndexAsc(
      UUID tenantId, UUID documentId);

  void deleteByTenantIdAndDocumentId(UUID tenantId, UUID documentId);
}
