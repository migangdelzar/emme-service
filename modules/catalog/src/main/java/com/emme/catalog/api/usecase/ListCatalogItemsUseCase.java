package com.emme.catalog.api.usecase;

import com.emme.catalog.api.query.ListCatalogItemsQuery;
import com.emme.catalog.api.result.CatalogItemInfo;
import java.util.List;

public interface ListCatalogItemsUseCase {
  List<CatalogItemInfo> list(ListCatalogItemsQuery query);
}
