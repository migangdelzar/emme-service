package com.emme.studio.application.service;

import com.emme.studio.api.usecase.ListCatalogServicesUseCase;
import com.emme.studio.application.port.out.ServiceRepository;
import com.emme.studio.domain.model.ServiceStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for filtered service-catalog listing. */
@Service
@Transactional(readOnly = true)
public class ListCatalogServicesService implements ListCatalogServicesUseCase {

  private final ServiceRepository serviceRepository;

  public ListCatalogServicesService(ServiceRepository serviceRepository) {
    this.serviceRepository = serviceRepository;
  }

  @Override
  public List<com.emme.studio.domain.model.Service> list(UUID tenantId, ServiceStatus status) {
    return serviceRepository.findByTenantIdAndStatus(tenantId, status);
  }
}
