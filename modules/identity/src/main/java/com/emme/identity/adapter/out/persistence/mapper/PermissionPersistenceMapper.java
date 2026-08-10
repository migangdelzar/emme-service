package com.emme.identity.adapter.out.persistence.mapper;

import com.emme.identity.adapter.out.persistence.entity.PermissionEntity;
import com.emme.identity.domain.model.Permission;
import org.springframework.stereotype.Component;

/** Translates permissions between the domain model and the JPA representation. */
@Component
public final class PermissionPersistenceMapper {

  public PermissionEntity toEntity(Permission permission) {
    if (permission.id() == null) {
      PermissionEntity entity =
          new PermissionEntity(permission.code(), permission.name(), permission.description());
      entity.setActive(permission.isActive());
      return entity;
    }
    return PermissionEntity.restore(
        permission.id(),
        permission.code(),
        permission.name(),
        permission.description(),
        permission.isActive(),
        permission.createdAt(),
        permission.updatedAt());
  }

  public Permission toDomain(PermissionEntity entity) {
    return Permission.rehydrate(
        entity.getId(),
        entity.getCode(),
        entity.getName(),
        entity.getDescription(),
        entity.isActive(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
