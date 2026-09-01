package com.emme.services.application.service;

import com.emme.ai.contracts.semantic.SemanticCacheDependencyChanged;
import com.emme.ai.contracts.semantic.SemanticCacheDependencyPublisher;
import com.emme.services.api.result.ServiceDetails;
import com.emme.services.api.usecase.RetireServiceCatalogEntryUseCase;
import com.emme.services.application.mapper.ServiceCatalogApplicationMapper;
import com.emme.services.application.port.out.ServiceRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for service-catalog retirement. */
@Service
@Transactional
public class RetireServiceCatalogEntryService implements RetireServiceCatalogEntryUseCase {

  private final ServiceRepository serviceRepository;
  private final SemanticCacheDependencyPublisher cacheDependencies;

  public RetireServiceCatalogEntryService(
      ServiceRepository serviceRepository, SemanticCacheDependencyPublisher cacheDependencies) {
    this.serviceRepository = serviceRepository;
    this.cacheDependencies = cacheDependencies;
  }

  @Override
  public ServiceDetails retire(UUID id) {
    com.emme.services.domain.model.Service service =
        serviceRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));
    service.retire();
    ServiceDetails details =
        ServiceCatalogApplicationMapper.toDetails(serviceRepository.save(service));
    cacheDependencies.publish(
        new SemanticCacheDependencyChanged(
            UUID.randomUUID(),
            service.getTenantId(),
            null,
            SemanticCacheDependencyChanged.Dependency.SERVICE,
            details.id().toString(),
            Instant.now()));
    return details;
  }
}
