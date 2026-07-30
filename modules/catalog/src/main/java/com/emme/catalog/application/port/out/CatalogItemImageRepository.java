package com.emme.catalog.application.port.out;

import com.emme.catalog.domain.model.CatalogItemImage;
import java.util.List;
import java.util.UUID;

public interface CatalogItemImageRepository {

  List<CatalogItemImage> findByCatalogItemId(UUID catalogItemId);

  List<CatalogItemImage> findAllById(Iterable<UUID> ids);

  CatalogItemImage save(CatalogItemImage image);

  void deleteAll(List<CatalogItemImage> images);
}
