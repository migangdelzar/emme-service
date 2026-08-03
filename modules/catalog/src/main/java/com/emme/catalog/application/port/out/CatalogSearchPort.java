package com.emme.catalog.application.port.out;

import java.util.List;
import java.util.UUID;

/** Search capability required by Catalog matching use cases. */
public interface CatalogSearchPort {

  List<CatalogSearchHit> searchCatalogItems(
      UUID tenantId, List<Float> queryVector, String queryText, int limit);

  List<CatalogSearchHit> searchCatalogItemImages(
      UUID tenantId, List<Float> queryVector, String queryText, int limit);
}
