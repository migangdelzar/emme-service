package com.emme.studio.application.service;

import com.emme.studio.api.usecase.GetServiceCatalogEntryUseCase;
import com.emme.studio.application.port.out.ServiceRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for service-catalog retrieval. */
@Service
@Transactional(readOnly = true)
public class GetServiceCatalogEntryService implements GetServiceCatalogEntryUseCase {

  private final ServiceRepository serviceRepository;

  public GetServiceCatalogEntryService(ServiceRepository serviceRepository) {
    this.serviceRepository = serviceRepository;
  }

  @Override
  public Optional<com.emme.studio.domain.model.Service> get(UUID id) {
    return serviceRepository.findById(id);
  }
}
