package com.emme.assistant.ai.adapter.out.persistence.repository;

import com.emme.assistant.ai.adapter.out.persistence.entity.DesignImageEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Module-private Spring Data repository for design-image metadata. */
@Repository
public interface SpringDataDesignImageRepository extends JpaRepository<DesignImageEntity, UUID> {

  void deleteByTenantIdAndWorkflowIdAndStorageKey(
      UUID tenantId, UUID workflowId, String storageKey);
}
