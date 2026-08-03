package com.emme.studio.application.service;

import com.emme.studio.api.usecase.RetireServiceCatalogEntryUseCase;
import com.emme.studio.application.port.out.ServiceRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for service-catalog retirement. */
@Service
@Transactional
public class RetireServiceCatalogEntryService implements RetireServiceCatalogEntryUseCase {

  private final ServiceRepository serviceRepository;

  public RetireServiceCatalogEntryService(ServiceRepository serviceRepository) {
    this.serviceRepository = serviceRepository;
  }

  @Override
  public com.emme.studio.domain.model.Service retire(UUID id) {
    com.emme.studio.domain.model.Service service =
        serviceRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));
    service.retire();
    return serviceRepository.save(service);
  }
}
