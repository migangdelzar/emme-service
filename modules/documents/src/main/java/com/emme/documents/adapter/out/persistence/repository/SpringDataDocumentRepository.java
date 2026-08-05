package com.emme.documents.adapter.out.persistence.repository;

import com.emme.documents.adapter.out.persistence.entity.DocumentEntity;
import com.emme.documents.adapter.out.persistence.entity.DocumentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataDocumentRepository extends JpaRepository<DocumentEntity, UUID> {

  List<DocumentEntity> findByTenantId(UUID tenantId);

  Optional<DocumentEntity> findByTenantIdAndId(UUID tenantId, UUID documentId);

  List<DocumentEntity> findByTenantIdAndStatus(UUID tenantId, DocumentStatus status);
}
