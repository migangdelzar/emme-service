package com.emme.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.domain.model.CustomerIdentity;
import com.emme.identity.domain.model.SocialProvider;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerIdentityTest {

  @Test
  void updatesProfileAndReportsWhetherPhoneIsRequired() {
    CustomerIdentity customer =
        CustomerIdentity.create(
            "customer@example.com", "Old Name", SocialProvider.GOOGLE, "provider-1", "old-avatar");

    assertThat(customer.needsPhone()).isTrue();
    customer.updateProfile("New Name", "new-avatar");
    customer.updatePhone("+5215550101");

    assertThat(customer.name()).isEqualTo("New Name");
    assertThat(customer.avatarUrl()).isEqualTo("new-avatar");
    assertThat(customer.phone()).isEqualTo("+5215550101");
    assertThat(customer.needsPhone()).isFalse();
  }

  @Test
  void rehydratesPersistedIdentityWithoutChangingItsCreationTime() {
    UUID id = UUID.randomUUID();
    var createdAt = java.time.Instant.parse("2026-01-01T00:00:00Z");
    var updatedAt = java.time.Instant.parse("2026-01-01T01:00:00Z");

    CustomerIdentity customer =
        CustomerIdentity.rehydrate(
            id,
            "customer@example.com",
            "Customer",
            "555",
            SocialProvider.FACEBOOK,
            "provider-1",
            "avatar",
            createdAt,
            updatedAt);

    assertThat(customer.id()).isEqualTo(id);
    assertThat(customer.createdAt()).isEqualTo(createdAt);
    assertThat(customer.updatedAt()).isEqualTo(updatedAt);
  }
}
