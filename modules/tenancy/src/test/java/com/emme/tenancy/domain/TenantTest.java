package com.emme.tenancy.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.tenancy.domain.model.Tenant;
import com.emme.tenancy.domain.model.TenantStatus;
import org.junit.jupiter.api.Test;

class TenantTest {

  @Test
  void createsAnActiveTenantWithTheDefaultIdentityRealm() {
    Tenant tenant = new Tenant("studio-a", "Studio A");

    assertThat(tenant.id()).isNull();
    assertThat(tenant.slug()).isEqualTo("studio-a");
    assertThat(tenant.name()).isEqualTo("Studio A");
    assertThat(tenant.status()).isEqualTo(TenantStatus.ACTIVE);
    assertThat(tenant.keycloakRealm()).isEqualTo("emme");
  }

  @Test
  void protectsTenantLifecycleTransitions() {
    Tenant tenant = new Tenant("studio-a", "Studio A");

    tenant.suspend();
    assertThat(tenant.status()).isEqualTo(TenantStatus.SUSPENDED);

    assertThatThrownBy(tenant::suspend)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SUSPENDED");

    tenant.reactivate();
    assertThat(tenant.status()).isEqualTo(TenantStatus.ACTIVE);

    assertThatThrownBy(tenant::reactivate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ACTIVE");
  }
}
