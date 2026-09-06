package com.emme.tenancy.adapter.out.persistence.adapter;

import com.emme.tenancy.adapter.out.persistence.entity.TenantRegistryEntity;
import com.emme.tenancy.adapter.out.persistence.repository.SpringDataTenantRegistryRepository;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TenantProvisioningPersistenceAdapter implements TenantProvisioningRepository {

  private final SpringDataTenantRegistryRepository repository;

  public TenantProvisioningPersistenceAdapter(SpringDataTenantRegistryRepository repository) {
    this.repository = repository;
  }

  @Override
  public UUID requestProvisioning(UUID tenantId, String slug, String schemaName) {
    var existing = repository.findBySlug(slug);
    if (existing.isPresent()) {
      return existing.get().getTenantId();
    }
    repository.save(new TenantRegistryEntity(tenantId, slug, schemaName, "PROVISIONING"));
    return tenantId;
  }

  @Override
  public TenantProvisioningStatus findStatus(UUID tenantId) {
    return repository
        .findByTenantId(tenantId)
        .map(
            e ->
                new TenantProvisioningStatus(
                    e.getStatus(), e.getSchemaName(), e.getLastMigratedAt(), e.getMigrationError()))
        .orElseThrow(() -> new IllegalArgumentException("Tenant registry not found: " + tenantId));
  }

  @Override
  public List<TenantProvisioningRequest> findPending() {
    return repository.findByStatus("PROVISIONING").stream()
        .map(e -> new TenantProvisioningRequest(e.getTenantId(), e.getSlug(), e.getSchemaName()))
        .toList();
  }

  @Override
  public void markActive(UUID tenantId) {
    repository
        .findByTenantId(tenantId)
        .ifPresent(
            entity -> {
              entity.setStatus("ACTIVE");
              entity.setSchemaVersion("0.1.0");
              entity.setLastMigratedAt(Instant.now());
              entity.setMigrationError(null);
              repository.save(entity);
            });
  }

  @Override
  public void markFailed(UUID tenantId, String error) {
    repository
        .findByTenantId(tenantId)
        .ifPresent(
            entity -> {
              entity.setStatus("FAILED");
              entity.setMigrationError(error);
              repository.save(entity);
            });
  }

  @Override
  public String findSchemaName(UUID tenantId) {
    return repository
        .findByTenantId(tenantId)
        .map(TenantRegistryEntity::getSchemaName)
        .orElseThrow(() -> new IllegalArgumentException("Tenant registry not found: " + tenantId));
  }
}
