package com.emme.catalog.api.usecase;

import com.emme.catalog.api.query.MatchCatalogItemsQuery;
import com.emme.catalog.api.result.CatalogMatchListInfo;

public interface MatchCatalogItemsUseCase {
  CatalogMatchListInfo match(MatchCatalogItemsQuery query);
}
