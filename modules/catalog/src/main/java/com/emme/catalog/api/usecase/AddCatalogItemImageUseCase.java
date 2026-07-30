package com.emme.catalog.api.usecase;

import com.emme.catalog.api.command.AddCatalogItemImageCommand;
import com.emme.catalog.api.result.CatalogItemImageInfo;

public interface AddCatalogItemImageUseCase {
  CatalogItemImageInfo addImage(AddCatalogItemImageCommand command);
}
