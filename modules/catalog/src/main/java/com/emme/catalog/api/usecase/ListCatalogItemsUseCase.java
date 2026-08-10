package com.emme.catalog.api.usecase;

import com.emme.catalog.api.query.ListCatalogItemsQuery;
import com.emme.catalog.api.result.CatalogItemDetails;
import java.util.List;

public interface ListCatalogItemsUseCase {
  List<CatalogItemDetails> list(ListCatalogItemsQuery query);
}
