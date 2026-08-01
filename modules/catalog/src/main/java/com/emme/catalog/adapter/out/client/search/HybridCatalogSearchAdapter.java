package com.emme.catalog.adapter.out.client.search;

import com.emme.catalog.application.port.out.CatalogSearchHit;
import com.emme.catalog.application.port.out.CatalogSearchPort;
import com.emme.shared.search.HybridSearch;
import com.emme.shared.search.SearchTarget;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Catalog-owned adapter that translates the shared hybrid-search engine into Catalog semantics. */
@Component
class HybridCatalogSearchAdapter implements CatalogSearchPort {

  private final HybridSearch hybridSearch;

  HybridCatalogSearchAdapter(HybridSearch hybridSearch) {
    this.hybridSearch = hybridSearch;
  }

  @Override
  public List<CatalogSearchHit> searchCatalogItems(
      UUID tenantId, List<Float> queryVector, String queryText, int limit) {
    return search(SearchTarget.CATALOG_ITEM, tenantId, queryVector, queryText, limit);
  }

  @Override
  public List<CatalogSearchHit> searchCatalogItemImages(
      UUID tenantId, List<Float> queryVector, String queryText, int limit) {
    return search(SearchTarget.CATALOG_ITEM_IMAGE, tenantId, queryVector, queryText, limit);
  }

  private List<CatalogSearchHit> search(
      SearchTarget target, UUID tenantId, List<Float> queryVector, String queryText, int limit) {
    return hybridSearch.search(target, tenantId, queryVector, queryText, limit).stream()
        .map(hit -> new CatalogSearchHit(hit.id(), hit.score()))
        .toList();
  }
}
