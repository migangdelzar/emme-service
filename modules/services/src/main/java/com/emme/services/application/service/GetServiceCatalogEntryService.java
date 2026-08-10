package com.emme.services.application.service;

import com.emme.services.api.result.ServiceDetails;
import com.emme.services.api.usecase.GetServiceCatalogEntryUseCase;
import com.emme.services.application.mapper.ServiceCatalogApplicationMapper;
import com.emme.services.application.port.out.ServiceRepository;
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
  public Optional<ServiceDetails> get(UUID id) {
    return serviceRepository.findById(id).map(ServiceCatalogApplicationMapper::toDetails);
  }
}
