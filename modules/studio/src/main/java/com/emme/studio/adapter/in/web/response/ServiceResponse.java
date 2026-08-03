package com.emme.studio.adapter.in.web.response;

import com.emme.studio.domain.model.Service;
import java.math.BigDecimal;
import java.util.UUID;

/** HTTP representation of a service catalog entry. */
public record ServiceResponse(
    UUID id,
    String code,
    String name,
    String category,
    String description,
    int durationMinutes,
    BigDecimal basePrice,
    String status) {

  public static ServiceResponse from(Service service) {
    return new ServiceResponse(
        service.getId(),
        service.getCode(),
        service.getName(),
        service.getCategory(),
        service.getDescription(),
        service.getDurationMinutes(),
        service.getBasePrice(),
        service.getStatus().name());
  }
}
