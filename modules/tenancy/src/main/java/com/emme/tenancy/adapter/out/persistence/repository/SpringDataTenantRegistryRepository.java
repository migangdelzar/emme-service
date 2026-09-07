package com.emme.tenancy.adapter.out.persistence.repository;

import com.emme.tenancy.adapter.out.persistence.entity.TenantRegistryEntity;
import com.emme.tenancy.domain.model.TenantProvisioningState;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataTenantRegistryRepository
    extends JpaRepository<TenantRegistryEntity, UUID> {

  Optional<TenantRegistryEntity> findByTenantId(UUID tenantId);

  Optional<TenantRegistryEntity> findBySlug(String slug);

  List<TenantRegistryEntity> findByStatus(TenantProvisioningState status);

  @Modifying
  @Query(
      "UPDATE TenantRegistryEntity e "
          + "SET e.status = :active, e.schemaVersion = :schemaVersion, "
          + "e.lastMigratedAt = :migratedAt, e.migrationError = null "
          + "WHERE e.tenantId = :tenantId AND e.status <> :active")
  int claimActivation(
      @Param("tenantId") UUID tenantId,
      @Param("active") TenantProvisioningState active,
      @Param("schemaVersion") String schemaVersion,
      @Param("migratedAt") Instant migratedAt);
}
