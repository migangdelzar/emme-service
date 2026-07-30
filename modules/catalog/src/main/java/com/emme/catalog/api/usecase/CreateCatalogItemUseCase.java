package com.emme.catalog.api.usecase;

import com.emme.catalog.api.command.CreateCatalogItemCommand;
import com.emme.catalog.api.result.CatalogItemInfo;

public interface CreateCatalogItemUseCase {
  CatalogItemInfo create(CreateCatalogItemCommand command);
}
