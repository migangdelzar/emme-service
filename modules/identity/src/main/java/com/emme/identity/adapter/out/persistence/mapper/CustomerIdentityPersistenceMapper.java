package com.emme.identity.adapter.out.persistence.mapper;

import com.emme.identity.adapter.out.persistence.entity.CustomerIdentityEntity;
import com.emme.identity.domain.model.CustomerIdentity;
import org.springframework.stereotype.Component;

/** Translates CustomerIdentity between domain and JPA representations. */
@Component
public final class CustomerIdentityPersistenceMapper {

  public CustomerIdentityEntity toEntity(CustomerIdentity customer) {
    return new CustomerIdentityEntity(
        customer.id(),
        customer.email(),
        customer.name(),
        customer.phone(),
        customer.provider(),
        customer.providerId(),
        customer.avatarUrl(),
        customer.createdAt(),
        customer.updatedAt());
  }

  public CustomerIdentity toDomain(CustomerIdentityEntity entity) {
    return CustomerIdentity.rehydrate(
        entity.getId(),
        entity.getEmail(),
        entity.getName(),
        entity.getPhone(),
        entity.getProvider(),
        entity.getProviderId(),
        entity.getAvatarUrl(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
