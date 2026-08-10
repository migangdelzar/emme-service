package com.emme.catalog.api.usecase;

import com.emme.catalog.api.query.MatchCatalogItemsQuery;
import com.emme.catalog.api.result.CatalogMatchList;

public interface MatchCatalogItemsUseCase {
  CatalogMatchList match(MatchCatalogItemsQuery query);
}
