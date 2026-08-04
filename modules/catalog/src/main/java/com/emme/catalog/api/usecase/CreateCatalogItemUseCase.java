package com.emme.catalog.api.usecase;

import com.emme.catalog.api.command.CreateCatalogItemCommand;
import com.emme.catalog.api.result.CatalogItemDetails;

public interface CreateCatalogItemUseCase {
  CatalogItemDetails create(CreateCatalogItemCommand command);
}
