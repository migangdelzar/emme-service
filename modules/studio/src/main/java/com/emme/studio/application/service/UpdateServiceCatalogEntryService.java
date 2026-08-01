package com.emme.studio.application.service;

import com.emme.studio.api.usecase.UpdateServiceCatalogEntryUseCase;
import com.emme.studio.application.port.out.ServiceRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for service-catalog updates. */
@Service
@Transactional
public class UpdateServiceCatalogEntryService implements UpdateServiceCatalogEntryUseCase {

  private final ServiceRepository serviceRepository;

  public UpdateServiceCatalogEntryService(ServiceRepository serviceRepository) {
    this.serviceRepository = serviceRepository;
  }

  @Override
  public com.emme.studio.domain.model.Service update(
      UUID id,
      String name,
      String category,
      String description,
      int durationMinutes,
      BigDecimal basePrice) {
    com.emme.studio.domain.model.Service service =
        serviceRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));
    service.setName(name);
    if (category != null) {
      service.setCategory(category);
    }
    if (description != null) {
      service.setDescription(description);
    }
    service.setDurationMinutes(durationMinutes);
    service.setBasePrice(basePrice);
    return serviceRepository.save(service);
  }
}
