package com.emme.catalog.adapter.in.web.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CatalogMatchResponse(List<Match> matches) {

  public record Match(
      UUID itemId, String name, BigDecimal price, double score, List<MatchedImage> matchedImages) {}

  public record MatchedImage(UUID imageId, String storageKey) {}
}
