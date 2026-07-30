package com.emme.studio.documents.entity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

  List<Document> findByTenantId(UUID tenantId);

  List<Document> findByTenantIdAndStatus(UUID tenantId, DocumentStatus status);
}
