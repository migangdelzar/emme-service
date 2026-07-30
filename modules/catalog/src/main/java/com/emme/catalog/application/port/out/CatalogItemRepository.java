package com.emme.catalog.application.port.out;

import com.emme.catalog.domain.model.CatalogItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogItemRepository {

  Optional<CatalogItem> findById(UUID id);

  List<CatalogItem> findByTenantId(UUID tenantId);

  CatalogItem save(CatalogItem item);

  void delete(CatalogItem item);
}
