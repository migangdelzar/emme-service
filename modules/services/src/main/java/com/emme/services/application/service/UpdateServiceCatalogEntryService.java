package com.emme.services.application.service;

import com.emme.ai.contracts.semantic.SemanticCacheDependencyChanged;
import com.emme.ai.contracts.semantic.SemanticCacheDependencyPublisher;
import com.emme.services.api.result.ServiceDetails;
import com.emme.services.api.usecase.UpdateServiceCatalogEntryUseCase;
import com.emme.services.application.mapper.ServiceCatalogApplicationMapper;
import com.emme.services.application.port.out.ServiceRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for service-catalog updates. */
@Service
@Transactional
public class UpdateServiceCatalogEntryService implements UpdateServiceCatalogEntryUseCase {

  private final ServiceRepository serviceRepository;
  private final SemanticCacheDependencyPublisher cacheDependencies;

  public UpdateServiceCatalogEntryService(
      ServiceRepository serviceRepository, SemanticCacheDependencyPublisher cacheDependencies) {
    this.serviceRepository = serviceRepository;
    this.cacheDependencies = cacheDependencies;
  }

  @Override
  public ServiceDetails update(
      UUID id,
      String name,
      String category,
      String description,
      int durationMinutes,
      BigDecimal basePrice) {
    com.emme.services.domain.model.Service service =
        serviceRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));
    BigDecimal previousPrice = service.getBasePrice();
    service.setName(name);
    if (category != null) {
      service.setCategory(category);
    }
    if (description != null) {
      service.setDescription(description);
    }
    service.setDurationMinutes(durationMinutes);
    service.setBasePrice(basePrice);
    ServiceDetails details =
        ServiceCatalogApplicationMapper.toDetails(serviceRepository.save(service));
    publish(details.id(), service.getTenantId(), SemanticCacheDependencyChanged.Dependency.SERVICE);
    if (previousPrice.compareTo(service.getBasePrice()) != 0) {
      publish(details.id(), service.getTenantId(), SemanticCacheDependencyChanged.Dependency.PRICE);
    }
    return details;
  }

  private void publish(
      UUID resourceId, UUID tenantId, SemanticCacheDependencyChanged.Dependency dependency) {
    cacheDependencies.publish(
        new SemanticCacheDependencyChanged(
            UUID.randomUUID(), tenantId, null, dependency, resourceId.toString(), Instant.now()));
  }
}
