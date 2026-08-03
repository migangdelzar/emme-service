package com.emme.studio.application.service;

import com.emme.studio.api.result.ServiceDetails;
import com.emme.studio.api.usecase.ListActiveServiceCatalogEntriesUseCase;
import com.emme.studio.application.mapper.ServiceCatalogApplicationMapper;
import com.emme.studio.application.port.out.ServiceRepository;
import com.emme.studio.domain.model.ServiceStatus;
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
