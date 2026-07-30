package com.emme.catalog.adapter.out.persistence.repository;

import com.emme.catalog.domain.model.CatalogItemImage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCatalogItemImageRepository
    extends JpaRepository<CatalogItemImage, UUID> {
  List<CatalogItemImage> findByCatalogItemId(UUID catalogItemId);
}
