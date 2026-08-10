package com.emme.tenancy.adapter.out.persistence.adapter;

import com.emme.tenancy.adapter.out.persistence.entity.TenantEntity;
import com.emme.tenancy.adapter.out.persistence.mapper.TenantPersistenceMapper;
import com.emme.tenancy.adapter.out.persistence.repository.SpringDataTenantRepository;
import com.emme.tenancy.application.port.out.TenantRepository;
import com.emme.tenancy.domain.model.Tenant;
import com.emme.tenancy.domain.model.TenantStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implements the application-owned tenant repository port with Spring Data JPA. */
@Component
public final class TenantPersistenceAdapter implements TenantRepository {

  private final SpringDataTenantRepository repository;
  private final TenantPersistenceMapper mapper;

  public TenantPersistenceAdapter(
      SpringDataTenantRepository repository, TenantPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public boolean existsBySlug(String slug) {
    return repository.existsBySlug(slug);
  }

  @Override
  public Tenant save(Tenant tenant) {
    TenantEntity entity =
        tenant.id() == null
            ? mapper.toEntity(tenant)
            : repository
                .findById(tenant.id())
                .map(existing -> update(existing, tenant))
                .orElseGet(() -> mapper.toEntity(tenant));
    return mapper.toDomain(repository.save(entity));
  }

  @Override
  public Optional<Tenant> findById(UUID tenantId) {
    return repository.findById(tenantId).map(mapper::toDomain);
  }

  @Override
  public Optional<Tenant> findBySlug(String slug) {
    return repository.findBySlug(slug).map(mapper::toDomain);
  }

  @Override
  public List<Tenant> findAll() {
    return repository.findAll().stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Tenant> findByStatus(TenantStatus status) {
    return repository.findByStatus(status).stream().map(mapper::toDomain).toList();
  }

  @Override
  public Optional<UUID> findDatabaseIdByTenantId(UUID tenantId) {
    return repository.findDatabaseIdByTenantId(tenantId);
  }

  private TenantEntity update(TenantEntity entity, Tenant tenant) {
    entity.setName(tenant.name());
    entity.setStatus(tenant.status());
    entity.setDatabaseId(tenant.databaseId());
    entity.setKeycloakRealm(tenant.keycloakRealm());
    return entity;
  }
}
