package com.emme.tenancy.adapter.out.persistence.repository;

import com.emme.tenancy.adapter.out.persistence.entity.TenantRegistryEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataTenantRegistryRepository
    extends JpaRepository<TenantRegistryEntity, UUID> {

  Optional<TenantRegistryEntity> findByTenantId(UUID tenantId);

  Optional<TenantRegistryEntity> findBySlug(String slug);

  List<TenantRegistryEntity> findByStatus(String status);
}
