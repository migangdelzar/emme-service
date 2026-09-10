package com.emme.services.application.service;

import com.emme.services.api.result.ServiceDetails;
import com.emme.services.api.usecase.GetServiceCatalogEntryUseCase;
import com.emme.services.application.mapper.ServiceCatalogApplicationMapper;
import com.emme.services.application.port.out.ServiceRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for service-catalog retrieval. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetServiceCatalogEntryService implements GetServiceCatalogEntryUseCase {

  private final ServiceRepository serviceRepository;

  @Override
  public Optional<ServiceDetails> get(UUID id) {
    return serviceRepository.findById(id).map(ServiceCatalogApplicationMapper::toDetails);
  }
}
