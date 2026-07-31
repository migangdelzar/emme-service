package com.emme.catalog.adapter.out.persistence.repository;

import com.emme.catalog.adapter.out.persistence.entity.CatalogItemImageEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCatalogItemImageRepository
    extends JpaRepository<CatalogItemImageEntity, UUID> {
  List<CatalogItemImageEntity> findByCatalogItemId(UUID catalogItemId);
}
