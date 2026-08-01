package com.emme.studio.application.service;

import com.emme.studio.entity.Service;
import com.emme.studio.entity.ServiceRepository;
import com.emme.studio.entity.ServiceStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
@Transactional
public class ServiceCatalogService {

  private final ServiceRepository serviceRepository;

  public ServiceCatalogService(ServiceRepository serviceRepository) {
    this.serviceRepository = serviceRepository;
  }

  public Service create(
      UUID tenantId, String code, String name, int durationMinutes, BigDecimal basePrice) {
    return create(
        tenantId, code, name, "Servicios Complementarios", null, durationMinutes, basePrice);
  }

  public Service create(
      UUID tenantId,
      String code,
      String name,
      String category,
      String description,
      int durationMinutes,
      BigDecimal basePrice) {
    Service service =
        new Service(tenantId, code, name, category, description, durationMinutes, basePrice);
    return serviceRepository.save(service);
  }

  public Service update(UUID id, String name, int durationMinutes, BigDecimal basePrice) {
    Service existing =
        serviceRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));
    return update(
        id, name, existing.getCategory(), existing.getDescription(), durationMinutes, basePrice);
  }

  public Service update(
      UUID id,
      String name,
      String category,
      String description,
      int durationMinutes,
      BigDecimal basePrice) {
    Service service =
        serviceRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));
    service.setName(name);
    service.setCategory(category);
    service.setDescription(description);
    service.setDurationMinutes(durationMinutes);
    service.setBasePrice(basePrice);
    return serviceRepository.save(service);
  }

  public Service retire(UUID id) {
    Service service =
        serviceRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));
    service.setStatus(ServiceStatus.RETIRED);
    return serviceRepository.save(service);
  }

  @Transactional(readOnly = true)
  public Optional<Service> findById(UUID id) {
    return serviceRepository.findById(id);
  }

  @Transactional(readOnly = true)
  public List<Service> findByTenantId(UUID tenantId, ServiceStatus status) {
    return serviceRepository.findByTenantIdAndStatus(tenantId, status);
  }

  @Transactional(readOnly = true)
  public List<Service> findActiveServices(UUID tenantId) {
    return serviceRepository.findByTenantIdAndStatus(tenantId, ServiceStatus.ACTIVE);
  }
}
