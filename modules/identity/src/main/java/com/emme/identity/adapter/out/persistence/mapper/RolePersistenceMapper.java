package com.emme.identity.adapter.out.persistence.mapper;

import com.emme.identity.adapter.out.persistence.entity.RoleEntity;
import com.emme.identity.domain.model.Role;
import org.springframework.stereotype.Component;

/** Translates roles between the domain model and the JPA representation. */
@Component
public final class RolePersistenceMapper {

  public RoleEntity toEntity(Role role) {
    if (role.id() == null) {
      RoleEntity entity = new RoleEntity(role.code(), role.name(), role.scope());
      entity.setActive(role.isActive());
      return entity;
    }
    return RoleEntity.restore(
        role.id(),
        role.code(),
        role.name(),
        role.scope(),
        role.isActive(),
        role.createdAt(),
        role.updatedAt());
  }

  public Role toDomain(RoleEntity entity) {
    return Role.rehydrate(
        entity.getId(),
        entity.getCode(),
        entity.getName(),
        entity.getScope(),
        entity.isActive(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
