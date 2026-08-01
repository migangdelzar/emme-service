package com.emme.studio.documents.adapter.out.persistence.repository;

import com.emme.studio.documents.adapter.out.persistence.entity.DocumentEntity;
import com.emme.studio.documents.adapter.out.persistence.entity.DocumentStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataDocumentRepository extends JpaRepository<DocumentEntity, UUID> {

  List<DocumentEntity> findByTenantId(UUID tenantId);

  List<DocumentEntity> findByTenantIdAndStatus(UUID tenantId, DocumentStatus status);
}
