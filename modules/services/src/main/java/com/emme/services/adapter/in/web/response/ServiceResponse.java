package com.emme.services.adapter.in.web.response;

import com.emme.services.api.result.ServiceDetails;
import com.emme.services.api.type.ServiceStatus;
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
    ServiceStatus status) {

  public static ServiceResponse from(ServiceDetails service) {
    return new ServiceResponse(
        service.id(),
        service.code(),
        service.name(),
        service.category(),
        service.description(),
        service.durationMinutes(),
        service.basePrice(),
        service.status());
  }
}
