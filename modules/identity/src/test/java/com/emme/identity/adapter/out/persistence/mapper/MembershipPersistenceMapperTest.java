package com.emme.identity.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.adapter.out.persistence.entity.MembershipEntity;
import com.emme.identity.adapter.out.persistence.entity.RoleEntity;
import com.emme.identity.adapter.out.persistence.entity.RoleScope;
import com.emme.identity.domain.model.Membership;
import com.emme.identity.domain.model.MembershipStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MembershipPersistenceMapperTest {

  private final MembershipPersistenceMapper mapper = new MembershipPersistenceMapper();

  @Test
  void preservesDomainStateWhenMappingPersistedMembership() {
    UUID membershipId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
    Instant updatedAt = Instant.parse("2026-01-01T01:00:00Z");
    RoleEntity role = new RoleEntity("tenant-owner", "Tenant owner", RoleScope.TENANT);
    role.onCreate();

    Membership domain =
        Membership.rehydrate(
            membershipId,
            tenantId,
            role.getId(),
            role.getCode(),
            "user-123",
            MembershipStatus.SUSPENDED,
            createdAt,
            updatedAt);

    MembershipEntity entity = mapper.toEntity(domain, role);
    Membership restored = mapper.toDomain(entity);

    assertThat(restored.id()).isEqualTo(membershipId);
    assertThat(restored.tenantId()).isEqualTo(tenantId);
    assertThat(restored.roleId()).isEqualTo(role.getId());
    assertThat(restored.roleCode()).isEqualTo("tenant-owner");
    assertThat(restored.userReference()).isEqualTo("user-123");
    assertThat(restored.status()).isEqualTo(MembershipStatus.SUSPENDED);
    assertThat(restored.createdAt()).isEqualTo(createdAt);
    assertThat(restored.updatedAt()).isEqualTo(updatedAt);
  }
}
