package com.emme.tenancy.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.tenancy.adapter.out.persistence.entity.TenantEntity;
import com.emme.tenancy.domain.model.Tenant;
import com.emme.tenancy.domain.model.TenantStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantPersistenceMapperTest {

  private final TenantPersistenceMapper mapper = new TenantPersistenceMapper();

  @Test
  void mapsTheTenantAggregateWithoutLeakingPersistenceTypes() {
    UUID tenantId = UUID.randomUUID();
    UUID databaseId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
    Instant updatedAt = Instant.parse("2026-01-02T00:00:00Z");
    Tenant tenant =
        Tenant.rehydrate(
            tenantId,
            "studio-a",
            "Studio A",
            TenantStatus.SUSPENDED,
            databaseId,
            "studio-a-realm",
            createdAt,
            updatedAt);

    TenantEntity entity = mapper.toEntity(tenant);
    Tenant restored = mapper.toDomain(entity);

    assertThat(entity).isNotInstanceOf(Tenant.class);
    assertThat(restored.id()).isEqualTo(tenantId);
    assertThat(restored.slug()).isEqualTo("studio-a");
    assertThat(restored.name()).isEqualTo("Studio A");
    assertThat(restored.status()).isEqualTo(TenantStatus.SUSPENDED);
    assertThat(restored.databaseId()).isEqualTo(databaseId);
    assertThat(restored.keycloakRealm()).isEqualTo("studio-a-realm");
    assertThat(restored.createdAt()).isEqualTo(createdAt);
    assertThat(restored.updatedAt()).isEqualTo(updatedAt);
  }
}
