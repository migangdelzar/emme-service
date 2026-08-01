package com.emme.catalog.adapter.in.web.mapper;

import com.emme.catalog.adapter.in.web.request.AddCatalogItemImageRequest;
import com.emme.catalog.adapter.in.web.request.CreateCatalogItemRequest;
import com.emme.catalog.adapter.in.web.request.MatchCatalogItemsRequest;
import com.emme.catalog.adapter.in.web.response.CatalogItemImageResponse;
import com.emme.catalog.adapter.in.web.response.CatalogItemResponse;
import com.emme.catalog.adapter.in.web.response.CatalogMatchResponse;
import com.emme.catalog.api.command.AddCatalogItemImageCommand;
import com.emme.catalog.api.command.CreateCatalogItemCommand;
import com.emme.catalog.api.command.DeleteCatalogItemCommand;
import com.emme.catalog.api.query.ListCatalogItemsQuery;
import com.emme.catalog.api.query.MatchCatalogItemsQuery;
import com.emme.catalog.api.result.CatalogItemImageInfo;
import com.emme.catalog.api.result.CatalogItemInfo;
import com.emme.catalog.api.result.CatalogMatchListInfo;
import java.util.UUID;

public final class CatalogWebMapper {

  public static CreateCatalogItemCommand toCommand(
      UUID tenantId, CreateCatalogItemRequest request) {
    return new CreateCatalogItemCommand(
        tenantId,
        request.serviceId(),
        request.code(),
        request.name(),
        request.description(),
        request.price(),
        request.priceNotes(),
        request.durationMinutes(),
        request.materials());
  }

  public static DeleteCatalogItemCommand toDeleteCommand(UUID tenantId, UUID itemId) {
    return new DeleteCatalogItemCommand(tenantId, itemId);
  }

  public static AddCatalogItemImageCommand toCommand(
      UUID tenantId, UUID itemId, AddCatalogItemImageRequest request) {
    return new AddCatalogItemImageCommand(tenantId, itemId, request.imageBase64());
  }

  public static ListCatalogItemsQuery toQuery(UUID tenantId) {
    return new ListCatalogItemsQuery(tenantId);
  }

  public static MatchCatalogItemsQuery toQuery(UUID tenantId, MatchCatalogItemsRequest request) {
    return new MatchCatalogItemsQuery(tenantId, request.query(), request.imageBase64());
  }

  public static CatalogItemResponse toResponse(CatalogItemInfo info) {
    return new CatalogItemResponse(
        info.id(),
        info.serviceId(),
        info.code(),
        info.name(),
        info.description(),
        info.price(),
        info.priceNotes(),
        info.durationMinutes(),
        info.materials(),
        info.status());
  }

  public static CatalogItemImageResponse toResponse(CatalogItemImageInfo info) {
    return new CatalogItemImageResponse(info.id(), info.storageKey(), info.caption());
  }

  public static CatalogMatchResponse toResponse(CatalogMatchListInfo info) {
    return new CatalogMatchResponse(
        info.matches().stream()
            .map(
                m ->
                    new CatalogMatchResponse.Match(
                        m.itemId(),
                        m.name(),
                        m.price(),
                        m.score(),
                        m.matchedImages().stream()
                            .map(
                                mi ->
                                    new CatalogMatchResponse.MatchedImage(
                                        mi.imageId(), mi.storageKey()))
                            .toList()))
            .toList());
  }

  private CatalogWebMapper() {}
}
