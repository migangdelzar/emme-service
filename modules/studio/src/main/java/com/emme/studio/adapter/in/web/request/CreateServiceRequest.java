package com.emme.studio.adapter.in.web.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** HTTP request for creating a service catalog entry. */
public record CreateServiceRequest(
    String code,
    String name,
    String category,
    String description,
    int durationMinutes,
    @NotNull BigDecimal basePrice) {}
