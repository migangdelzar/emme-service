package com.emme.shared.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantOwnedEntityTest {

  private static final class TestTenantEntity extends TenantOwnedEntity {
    TestTenantEntity() {
      super();
    }

    TestTenantEntity(UUID tenantId) {
      super(tenantId);
    }
  }

  @Test
  void shouldStoreTenantId() {
    UUID tenantId = UUID.randomUUID();
    TestTenantEntity entity = new TestTenantEntity(tenantId);

    assertThat(entity.getTenantId()).isEqualTo(tenantId);
  }

  @Test
  void shouldInheritPersistedEntityBehavior() {
    TestTenantEntity entity = new TestTenantEntity();
    entity.onCreate();

    assertThat(entity.getId()).isNotNull();
    assertThat(entity.getId().version()).isEqualTo(7);
    assertThat(entity.getCreatedAt()).isNotNull();
    assertThat(entity.getUpdatedAt()).isNotNull();
  }

  @Test
  void shouldHaveEmptyTenantIdByDefault() {
    TestTenantEntity entity = new TestTenantEntity();

    assertThat(entity.getTenantId()).isNull();
  }
}
