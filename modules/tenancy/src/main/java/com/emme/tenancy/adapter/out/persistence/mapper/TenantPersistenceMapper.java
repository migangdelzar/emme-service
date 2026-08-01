package com.emme.tenancy.adapter.out.persistence.mapper;

import com.emme.tenancy.adapter.out.persistence.entity.TenantEntity;
import com.emme.tenancy.domain.model.Tenant;
import org.springframework.stereotype.Component;

/** Translates the Tenant aggregate to and from its JPA persistence representation. */
@Component
public final class TenantPersistenceMapper {

  public TenantEntity toEntity(Tenant tenant) {
    if (tenant.id() == null) {
      TenantEntity entity = new TenantEntity();
      entity.setSlug(tenant.slug());
      entity.setName(tenant.name());
      entity.setStatus(tenant.status());
      entity.setDatabaseId(tenant.databaseId());
      entity.setKeycloakRealm(tenant.keycloakRealm());
      return entity;
    }
    return TenantEntity.restore(
        tenant.id(),
        tenant.slug(),
        tenant.name(),
        tenant.status(),
        tenant.databaseId(),
        tenant.keycloakRealm(),
        tenant.createdAt(),
        tenant.updatedAt());
  }

  public Tenant toDomain(TenantEntity entity) {
    return Tenant.rehydrate(
        entity.getId(),
        entity.getSlug(),
        entity.getName(),
        entity.getStatus(),
        entity.getDatabaseId(),
        entity.getKeycloakRealm(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
