package com.emme.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.domain.model.Role;
import com.emme.identity.domain.model.RoleScope;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoleTest {

  @Test
  void createsAnActiveTenantRole() {
    Role role = new Role("tenant-owner", "Tenant owner", RoleScope.TENANT);

    assertThat(role.id()).isNull();
    assertThat(role.code()).isEqualTo("tenant-owner");
    assertThat(role.name()).isEqualTo("Tenant owner");
    assertThat(role.scope()).isEqualTo(RoleScope.TENANT);
    assertThat(role.isActive()).isTrue();
  }

  @Test
  void rehydratesAndCanBeDeactivated() {
    UUID id = UUID.randomUUID();

    Role role =
        Role.rehydrate(
            id,
            "admin",
            "Administrator",
            RoleScope.PLATFORM,
            true,
            java.time.Instant.EPOCH,
            java.time.Instant.EPOCH);

    role.deactivate();

    assertThat(role.id()).isEqualTo(id);
    assertThat(role.isActive()).isFalse();
  }
}
