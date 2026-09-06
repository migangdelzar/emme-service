package com.emme.services.application.mapper;

import com.emme.services.api.result.ServiceDetails;
import com.emme.services.api.type.ServiceStatus;
import com.emme.services.domain.model.Service;

/** Maps service-catalog domain objects to stable public use-case results. */
public final class ServiceCatalogApplicationMapper {

  private ServiceCatalogApplicationMapper() {}

  public static ServiceDetails toDetails(Service service) {
    return new ServiceDetails(
        service.getId(),
        service.getCode(),
        service.getName(),
        service.getCategory(),
        service.getDescription(),
        service.getDurationMinutes(),
        service.getBasePrice(),
        ServiceStatus.valueOf(service.getStatus().name()));
  }
}
