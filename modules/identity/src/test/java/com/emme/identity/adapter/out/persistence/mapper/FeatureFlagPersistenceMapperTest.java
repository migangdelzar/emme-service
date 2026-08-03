package com.emme.identity.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.adapter.out.persistence.entity.FeatureFlagEntity;
import com.emme.identity.domain.model.FeatureFlag;
import com.emme.studio.subscriptions.api.type.PlanType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FeatureFlagPersistenceMapperTest {

  private final FeatureFlagPersistenceMapper mapper = new FeatureFlagPersistenceMapper();

  @Test
  void preservesFeatureFlagStateWhenMappingPersistedData() {
    UUID id = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
    Instant updatedAt = Instant.parse("2026-01-01T01:00:00Z");
    FeatureFlag domain =
        FeatureFlag.rehydrate(
            id,
            tenantId,
            "calendar_sync",
            true,
            PlanType.PRO,
            "Calendar access",
            createdAt,
            updatedAt);

    FeatureFlagEntity entity = mapper.toEntity(domain);
    FeatureFlag restored = mapper.toDomain(entity);

    assertThat(restored.id()).isEqualTo(id);
    assertThat(restored.tenantId()).isEqualTo(tenantId);
    assertThat(restored.code()).isEqualTo("calendar_sync");
    assertThat(restored.isEnabled()).isTrue();
    assertThat(restored.planRequired()).isEqualTo(PlanType.PRO);
    assertThat(restored.description()).isEqualTo("Calendar access");
    assertThat(restored.createdAt()).isEqualTo(createdAt);
    assertThat(restored.updatedAt()).isEqualTo(updatedAt);
  }
}
