package com.emme.catalog.api.usecase;

import com.emme.catalog.api.command.DeleteCatalogItemCommand;

public interface DeleteCatalogItemUseCase {
  void delete(DeleteCatalogItemCommand command);
}
