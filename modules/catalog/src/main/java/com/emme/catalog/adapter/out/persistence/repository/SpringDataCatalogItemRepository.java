package com.emme.catalog.adapter.out.persistence.repository;

import com.emme.catalog.adapter.out.persistence.entity.CatalogItemEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCatalogItemRepository extends JpaRepository<CatalogItemEntity, UUID> {
  List<CatalogItemEntity> findByTenantId(UUID tenantId);
}
