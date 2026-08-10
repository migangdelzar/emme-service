package com.emme.services.application.service;

import com.emme.services.api.result.ServiceDetails;
import com.emme.services.api.usecase.RetireServiceCatalogEntryUseCase;
import com.emme.services.application.mapper.ServiceCatalogApplicationMapper;
import com.emme.services.application.port.out.ServiceRepository;
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
  public ServiceDetails retire(UUID id) {
    com.emme.services.domain.model.Service service =
        serviceRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));
    service.retire();
    return ServiceCatalogApplicationMapper.toDetails(serviceRepository.save(service));
  }
}
