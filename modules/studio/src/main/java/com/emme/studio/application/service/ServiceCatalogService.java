package com.emme.studio.application.service;

import com.emme.studio.adapter.out.persistence.entity.ServiceEntity;
import com.emme.studio.adapter.out.persistence.repository.SpringDataServiceRepository;
import com.emme.studio.domain.model.ServiceStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
@Transactional
public class ServiceCatalogService {

  private final SpringDataServiceRepository serviceRepository;

  public ServiceCatalogService(SpringDataServiceRepository serviceRepository) {
    this.serviceRepository = serviceRepository;
  }

  public ServiceEntity create(
      UUID tenantId, String code, String name, int durationMinutes, BigDecimal basePrice) {
    return create(
        tenantId, code, name, "Servicios Complementarios", null, durationMinutes, basePrice);
  }

  public ServiceEntity create(
      UUID tenantId,
      String code,
      String name,
      String category,
      String description,
      int durationMinutes,
      BigDecimal basePrice) {
    ServiceEntity service =
        new ServiceEntity(tenantId, code, name, category, description, durationMinutes, basePrice);
    return serviceRepository.save(service);
  }

  public ServiceEntity update(UUID id, String name, int durationMinutes, BigDecimal basePrice) {
    ServiceEntity existing =
        serviceRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("ServiceEntity not found: " + id));
    return update(
        id, name, existing.getCategory(), existing.getDescription(), durationMinutes, basePrice);
  }

  public ServiceEntity update(
      UUID id,
      String name,
      String category,
      String description,
      int durationMinutes,
      BigDecimal basePrice) {
    ServiceEntity service =
        serviceRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("ServiceEntity not found: " + id));
    service.setName(name);
    service.setCategory(category);
    service.setDescription(description);
    service.setDurationMinutes(durationMinutes);
    service.setBasePrice(basePrice);
    return serviceRepository.save(service);
  }

  public ServiceEntity retire(UUID id) {
    ServiceEntity service =
        serviceRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("ServiceEntity not found: " + id));
    service.setStatus(ServiceStatus.RETIRED);
    return serviceRepository.save(service);
  }

  @Transactional(readOnly = true)
  public Optional<ServiceEntity> findById(UUID id) {
    return serviceRepository.findById(id);
  }

  @Transactional(readOnly = true)
  public List<ServiceEntity> findByTenantId(UUID tenantId, ServiceStatus status) {
    return serviceRepository.findByTenantIdAndStatus(tenantId, status);
  }

  @Transactional(readOnly = true)
  public List<ServiceEntity> findActiveServices(UUID tenantId) {
    return serviceRepository.findByTenantIdAndStatus(tenantId, ServiceStatus.ACTIVE);
  }
}
