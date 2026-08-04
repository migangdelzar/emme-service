package com.emme.catalog.application.mapper;

import com.emme.catalog.api.result.CatalogItemDetails;
import com.emme.catalog.api.result.CatalogItemImageDetails;
import com.emme.catalog.domain.model.CatalogItem;
import com.emme.catalog.domain.model.CatalogItemImage;

public final class CatalogApplicationMapper {

  public static CatalogItemDetails toResult(CatalogItem item) {
    return new CatalogItemDetails(
        item.getId(),
        item.getServiceId(),
        item.getCode(),
        item.getName(),
        item.getDescription(),
        item.getPrice(),
        item.getPriceNotes(),
        item.getDurationMinutes(),
        item.getMaterials(),
        item.getStatus().name());
  }

  public static CatalogItemImageDetails toResult(CatalogItemImage image) {
    return new CatalogItemImageDetails(
        image.getId(), image.getCatalogItemId(), image.getStorageKey(), image.getCaption());
  }

  private CatalogApplicationMapper() {}
}
