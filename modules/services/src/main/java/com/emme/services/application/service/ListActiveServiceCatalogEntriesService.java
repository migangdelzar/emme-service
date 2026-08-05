package com.emme.services.application.service;

import com.emme.services.api.result.ServiceDetails;
import com.emme.services.api.usecase.ListActiveServiceCatalogEntriesUseCase;
import com.emme.services.application.mapper.ServiceCatalogApplicationMapper;
import com.emme.services.application.port.out.ServiceRepository;
import com.emme.services.domain.model.ServiceStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for active service-catalog listing. */
@Service
@Transactional(readOnly = true)
public class ListActiveServiceCatalogEntriesService
    implements ListActiveServiceCatalogEntriesUseCase {

  private final ServiceRepository serviceRepository;

  public ListActiveServiceCatalogEntriesService(ServiceRepository serviceRepository) {
    this.serviceRepository = serviceRepository;
  }

  @Override
  public List<ServiceDetails> listActive(UUID tenantId) {
    return serviceRepository.findByTenantIdAndStatus(tenantId, ServiceStatus.ACTIVE).stream()
        .map(ServiceCatalogApplicationMapper::toDetails)
        .toList();
  }
}
