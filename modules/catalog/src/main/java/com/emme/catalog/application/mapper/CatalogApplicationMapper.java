package com.emme.catalog.application.mapper;

import com.emme.catalog.api.result.CatalogItemImageInfo;
import com.emme.catalog.api.result.CatalogItemInfo;
import com.emme.catalog.domain.model.CatalogItem;
import com.emme.catalog.domain.model.CatalogItemImage;

public final class CatalogApplicationMapper {

  public static CatalogItemInfo toInfo(CatalogItem item) {
    return new CatalogItemInfo(
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

  public static CatalogItemImageInfo toInfo(CatalogItemImage image) {
    return new CatalogItemImageInfo(
        image.getId(), image.getCatalogItemId(), image.getStorageKey(), image.getCaption());
  }

  private CatalogApplicationMapper() {}
}
