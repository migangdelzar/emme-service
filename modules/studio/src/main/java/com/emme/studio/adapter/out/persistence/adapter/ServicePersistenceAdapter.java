package com.emme.studio.adapter.out.persistence.adapter;

import com.emme.studio.adapter.out.persistence.entity.ServiceEntity;
import com.emme.studio.adapter.out.persistence.mapper.ServicePersistenceMapper;
import com.emme.studio.adapter.out.persistence.repository.SpringDataServiceRepository;
import com.emme.studio.application.port.out.ServiceRepository;
import com.emme.studio.domain.model.Service;
import com.emme.studio.domain.model.ServiceStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implements the Service Catalog persistence port using Spring Data JPA. */
@Component
public class ServicePersistenceAdapter implements ServiceRepository {

  private final SpringDataServiceRepository repository;
  private final ServicePersistenceMapper mapper;

  public ServicePersistenceAdapter(SpringDataServiceRepository repository) {
    this.repository = repository;
    this.mapper = new ServicePersistenceMapper();
  }

  @Override
  public Service save(Service service) {
    ServiceEntity entity =
        service.getId() == null
            ? mapper.toNewEntity(service)
            : repository.findById(service.getId()).orElseThrow();
    mapper.updateEntity(service, entity);
    return mapper.toDomain(repository.save(entity));
  }

  @Override
  public Optional<Service> findById(UUID id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<Service> findByTenantIdAndStatus(UUID tenantId, ServiceStatus status) {
    return repository.findByTenantIdAndStatus(tenantId, status).stream()
        .map(mapper::toDomain)
        .toList();
  }
}
