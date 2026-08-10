package com.emme.identity.adapter.out.persistence.mapper;

import com.emme.identity.adapter.out.persistence.entity.CustomerMembershipEntity;
import com.emme.identity.domain.model.CustomerMembership;
import org.springframework.stereotype.Component;

/** Translates CustomerMembership between domain and JPA representations. */
@Component
public final class CustomerMembershipPersistenceMapper {

  public CustomerMembershipEntity toEntity(CustomerMembership membership) {
    return new CustomerMembershipEntity(
        membership.customerId(), membership.tenantId(), membership.createdAt());
  }

  public CustomerMembership toDomain(CustomerMembershipEntity entity) {
    return CustomerMembership.rehydrate(
        entity.getCustomerId(), entity.getTenantId(), entity.getCreatedAt());
  }
}
