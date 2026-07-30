package com.emme.catalog.api.result;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CatalogMatchInfo(
    UUID itemId, String name, BigDecimal price, double score, List<MatchedImageInfo> matchedImages) {}
