package com.emme.catalog.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateCatalogItemRequest(
    @NotNull UUID serviceId,
    @NotBlank String code,
    @NotBlank String name,
    String description,
    @NotNull BigDecimal price,
    String priceNotes,
    Integer durationMinutes,
    String materials) {}
