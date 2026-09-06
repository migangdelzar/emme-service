package com.emme.services.api.result;

import com.emme.services.domain.model.ServiceStatus;
import java.math.BigDecimal;
import java.util.UUID;

/** Stable public service-catalog representation returned by Studio use cases. */
public record ServiceDetails(
    UUID id,
    String code,
    String name,
    String category,
    String description,
    int durationMinutes,
    BigDecimal basePrice,
    ServiceStatus status) {}
