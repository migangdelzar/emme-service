package com.emme.studio.application.service;

import com.emme.studio.api.result.ServiceDetails;
import com.emme.studio.api.usecase.GetServiceCatalogEntryUseCase;
import com.emme.studio.application.mapper.ServiceCatalogApplicationMapper;
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
  public Optional<ServiceDetails> get(UUID id) {
    return serviceRepository.findById(id).map(ServiceCatalogApplicationMapper::toDetails);
  }
}
