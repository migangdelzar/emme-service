package com.emme.studio.application.service;

import com.emme.studio.api.usecase.ListActiveCatalogServicesUseCase;
import com.emme.studio.application.port.out.ServiceRepository;
import com.emme.studio.domain.model.ServiceStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for active service-catalog listing. */
@Service
@Transactional(readOnly = true)
public class ListActiveCatalogServicesService implements ListActiveCatalogServicesUseCase {

  private final ServiceRepository serviceRepository;

  public ListActiveCatalogServicesService(ServiceRepository serviceRepository) {
    this.serviceRepository = serviceRepository;
  }

  @Override
  public List<com.emme.studio.domain.model.Service> listActive(UUID tenantId) {
    return serviceRepository.findByTenantIdAndStatus(tenantId, ServiceStatus.ACTIVE);
  }
}
