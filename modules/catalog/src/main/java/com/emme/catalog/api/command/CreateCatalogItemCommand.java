package com.emme.catalog.api.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateCatalogItemCommand(
    UUID tenantId,
    UUID serviceId,
    String code,
    String name,
    String description,
    BigDecimal price,
    String priceNotes,
    Integer durationMinutes,
    String materials) {}
