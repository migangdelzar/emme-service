package com.emme.studio.application.service;

import com.emme.studio.api.usecase.CreateCatalogServiceUseCase;
import com.emme.studio.application.port.out.ServiceRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for service-catalog creation. */
@Service
@Transactional
public class CreateCatalogServiceService implements CreateCatalogServiceUseCase {

  private static final String DEFAULT_CATEGORY = "Servicios Complementarios";
  private final ServiceRepository serviceRepository;

  public CreateCatalogServiceService(ServiceRepository serviceRepository) {
    this.serviceRepository = serviceRepository;
  }

  @Override
  public com.emme.studio.domain.model.Service create(
      UUID tenantId,
      String code,
      String name,
      String category,
      String description,
      int durationMinutes,
      BigDecimal basePrice) {
    String effectiveCategory = category == null ? DEFAULT_CATEGORY : category;
    return serviceRepository.save(
        new com.emme.studio.domain.model.Service(
            tenantId, code, name, effectiveCategory, description, durationMinutes, basePrice));
  }
}
