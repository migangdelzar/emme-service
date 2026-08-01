package com.emme.identity.adapter.out.persistence.mapper;

import com.emme.identity.adapter.out.persistence.entity.MembershipEntity;
import com.emme.identity.adapter.out.persistence.entity.Role;
import com.emme.identity.domain.model.Membership;
import org.springframework.stereotype.Component;

/** Translates the Membership aggregate to and from its JPA representation. */
@Component
public final class MembershipPersistenceMapper {

  public MembershipEntity toEntity(Membership membership, Role role) {
    if (membership.id() == null) {
      return new MembershipEntity(membership.tenantId(), role, membership.userReference());
    }
    return MembershipEntity.restore(
        membership.id(),
        membership.tenantId(),
        role,
        membership.userReference(),
        membership.status(),
        membership.createdAt(),
        membership.updatedAt());
  }

  public Membership toDomain(MembershipEntity entity) {
    return Membership.rehydrate(
        entity.getId(),
        entity.getTenantId(),
        entity.getRole().getId(),
        entity.getRole().getCode(),
        entity.getUserReference(),
        entity.getStatus(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
