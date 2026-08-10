package com.emme.catalog.api.exception;

import java.util.UUID;

public final class CatalogItemNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public CatalogItemNotFoundException(UUID itemId) {
    super("Catalog item not found: " + itemId);
  }
}
