package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.assistant.ai.adapter.out.persistence.entity.DesignImageEntity;
import com.emme.assistant.ai.adapter.out.persistence.repository.SpringDataDesignImageRepository;
import com.emme.assistant.ai.application.port.out.DesignImageMetadataRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** JPA adapter for stable design-image metadata CRUD. */
@Component
public final class JpaDesignImageMetadataRepository implements DesignImageMetadataRepository {

  private final SpringDataDesignImageRepository repository;

  public JpaDesignImageMetadataRepository(SpringDataDesignImageRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository must not be null");
  }

  @Override
  public void save(
      UUID tenantId, UUID workflowId, String storageKey, String mediaType, long sizeBytes) {
    repository.save(new DesignImageEntity(tenantId, workflowId, storageKey, mediaType, sizeBytes));
  }

  @Override
  public void delete(UUID tenantId, UUID workflowId, String storageKey) {
    repository.deleteByTenantIdAndWorkflowIdAndStorageKey(tenantId, workflowId, storageKey);
  }
}
