package com.emme.catalog.api.result;

import com.emme.catalog.domain.model.CatalogItemStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record CatalogItemDetails(
    UUID id,
    UUID serviceId,
    String code,
    String name,
    String description,
    BigDecimal price,
    String priceNotes,
    Integer durationMinutes,
    String materials,
    CatalogItemStatus status) {}
