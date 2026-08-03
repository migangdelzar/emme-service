package com.emme.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.domain.model.Permission;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PermissionTest {

  @Test
  void createsAnActivePermission() {
    Permission permission = new Permission("quotes.read", "Read quotes", "Read quote data");

    assertThat(permission.id()).isNull();
    assertThat(permission.code()).isEqualTo("quotes.read");
    assertThat(permission.name()).isEqualTo("Read quotes");
    assertThat(permission.description()).isEqualTo("Read quote data");
    assertThat(permission.isActive()).isTrue();
  }

  @Test
  void rehydratesAndCanBeDeactivated() {
    UUID id = UUID.randomUUID();

    Permission permission =
        Permission.rehydrate(
            id,
            "quotes.read",
            "Read quotes",
            "Read quote data",
            true,
            java.time.Instant.EPOCH,
            java.time.Instant.EPOCH);

    permission.deactivate();

    assertThat(permission.id()).isEqualTo(id);
    assertThat(permission.isActive()).isFalse();
  }
}
