package com.emme.catalog.api.exception;

import java.util.UUID;

public final class CatalogItemNotFoundException extends RuntimeException {

  public CatalogItemNotFoundException(UUID itemId) {
    super("Catalog item not found: " + itemId);
  }
}
