package com.emme.catalog.api.usecase;

import com.emme.catalog.api.command.AddCatalogItemImageCommand;
import com.emme.catalog.api.result.CatalogItemImageDetails;

public interface AddCatalogItemImageUseCase {
  CatalogItemImageDetails addImage(AddCatalogItemImageCommand command);
}
