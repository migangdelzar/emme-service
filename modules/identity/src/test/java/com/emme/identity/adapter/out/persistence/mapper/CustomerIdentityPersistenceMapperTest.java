package com.emme.identity.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.adapter.out.persistence.entity.CustomerIdentityEntity;
import com.emme.identity.domain.model.CustomerIdentity;
import com.emme.identity.domain.model.SocialProvider;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerIdentityPersistenceMapperTest {

  private final CustomerIdentityPersistenceMapper mapper = new CustomerIdentityPersistenceMapper();

  @Test
  void preservesIdentityStateWhenMappingPersistedCustomer() {
    CustomerIdentity customer =
        CustomerIdentity.rehydrate(
            UUID.randomUUID(),
            "customer@example.com",
            "Customer",
            "+5215550101",
            SocialProvider.GOOGLE,
            "provider-1",
            "avatar",
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T01:00:00Z"));

    CustomerIdentityEntity entity = mapper.toEntity(customer);
    CustomerIdentity restored = mapper.toDomain(entity);

    assertThat(restored.id()).isEqualTo(customer.id());
    assertThat(restored.email()).isEqualTo(customer.email());
    assertThat(restored.phone()).isEqualTo(customer.phone());
    assertThat(restored.provider()).isEqualTo(customer.provider());
    assertThat(restored.createdAt()).isEqualTo(customer.createdAt());
    assertThat(restored.updatedAt()).isEqualTo(customer.updatedAt());
  }
}
