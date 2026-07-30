package com.emme.catalog.adapter.out.persistence.repository;

import com.emme.catalog.domain.model.CatalogItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCatalogItemRepository extends JpaRepository<CatalogItem, UUID> {
  List<CatalogItem> findByTenantId(UUID tenantId);
}
