package com.emme.studio.application.mapper;

import com.emme.studio.api.result.ServiceDetails;
import com.emme.studio.domain.model.Service;

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
        service.getStatus().name());
  }
}
