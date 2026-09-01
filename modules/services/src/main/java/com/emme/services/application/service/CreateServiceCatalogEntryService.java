package com.emme.services.application.service;

import com.emme.ai.contracts.semantic.SemanticCacheDependencyChanged;
import com.emme.ai.contracts.semantic.SemanticCacheDependencyPublisher;
import com.emme.services.api.result.ServiceDetails;
import com.emme.services.api.usecase.CreateServiceCatalogEntryUseCase;
import com.emme.services.application.mapper.ServiceCatalogApplicationMapper;
import com.emme.services.application.port.out.ServiceRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for service-catalog creation. */
@Service
@Transactional
public class CreateServiceCatalogEntryService implements CreateServiceCatalogEntryUseCase {

  private static final String DEFAULT_CATEGORY = "Servicios Complementarios";
  private final ServiceRepository serviceRepository;
  private final SemanticCacheDependencyPublisher cacheDependencies;

  public CreateServiceCatalogEntryService(
      ServiceRepository serviceRepository, SemanticCacheDependencyPublisher cacheDependencies) {
    this.serviceRepository = serviceRepository;
    this.cacheDependencies = cacheDependencies;
  }

  @Override
  public ServiceDetails create(
      UUID tenantId,
      String code,
      String name,
      String category,
      String description,
      int durationMinutes,
      BigDecimal basePrice) {
    String effectiveCategory = category == null ? DEFAULT_CATEGORY : category;
    ServiceDetails details =
        ServiceCatalogApplicationMapper.toDetails(
            serviceRepository.save(
                new com.emme.services.domain.model.Service(
                    tenantId,
                    code,
                    name,
                    effectiveCategory,
                    description,
                    durationMinutes,
                    basePrice)));
    publish(details.id(), tenantId, SemanticCacheDependencyChanged.Dependency.SERVICE);
    publish(details.id(), tenantId, SemanticCacheDependencyChanged.Dependency.PRICE);
    return details;
  }

  private void publish(
      UUID resourceId, UUID tenantId, SemanticCacheDependencyChanged.Dependency dependency) {
    cacheDependencies.publish(
        new SemanticCacheDependencyChanged(
            UUID.randomUUID(), tenantId, null, dependency, resourceId.toString(), Instant.now()));
  }
}
