package com.emme.catalog.adapter.in.web.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogItemResponse(
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
