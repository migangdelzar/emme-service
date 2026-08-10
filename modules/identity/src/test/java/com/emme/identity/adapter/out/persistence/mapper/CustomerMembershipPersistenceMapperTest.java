package com.emme.identity.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.adapter.out.persistence.entity.CustomerMembershipEntity;
import com.emme.identity.domain.model.CustomerMembership;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerMembershipPersistenceMapperTest {

  private final CustomerMembershipPersistenceMapper mapper =
      new CustomerMembershipPersistenceMapper();

  @Test
  void preservesMembershipIdentityAndCreationTimeWhenMappingBothDirections() {
    UUID customerId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
    CustomerMembership membership = CustomerMembership.rehydrate(customerId, tenantId, createdAt);

    CustomerMembershipEntity entity = mapper.toEntity(membership);
    CustomerMembership restored = mapper.toDomain(entity);

    assertThat(restored.customerId()).isEqualTo(customerId);
    assertThat(restored.tenantId()).isEqualTo(tenantId);
    assertThat(restored.createdAt()).isEqualTo(createdAt);
  }
}
