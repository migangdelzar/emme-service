package com.emme.catalog.api.result;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogItemInfo(
    UUID id,
    UUID serviceId,
    String code,
    String name,
    String description,
    BigDecimal price,
    String priceNotes,
    Integer durationMinutes,
    String materials,
    String status) {}
