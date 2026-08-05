package com.emme.services.application.service;

import com.emme.services.api.result.ServiceDetails;
import com.emme.services.api.usecase.CreateServiceCatalogEntryUseCase;
import com.emme.services.application.mapper.ServiceCatalogApplicationMapper;
import com.emme.services.application.port.out.ServiceRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for service-catalog creation. */
@Service
@Transactional
public class CreateServiceCatalogEntryService implements CreateServiceCatalogEntryUseCase {

  private static final String DEFAULT_CATEGORY = "Servicios Complementarios";
  private final ServiceRepository serviceRepository;

  public CreateServiceCatalogEntryService(ServiceRepository serviceRepository) {
    this.serviceRepository = serviceRepository;
  }

  @Override
  public ServiceDetails create(
      UUID tenantId,
      String code,
      String name,
      String category,
      String description,
      int durationMinutes,
      BigDecimal basePrice) {
    String effectiveCategory = category == null ? DEFAULT_CATEGORY : category;
    return ServiceCatalogApplicationMapper.toDetails(
        serviceRepository.save(
            new com.emme.services.domain.model.Service(
                tenantId, code, name, effectiveCategory, description, durationMinutes, basePrice)));
  }
}
