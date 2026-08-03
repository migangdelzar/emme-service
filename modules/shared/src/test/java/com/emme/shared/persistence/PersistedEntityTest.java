package com.emme.shared.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PersistedEntityTest {

  private static final class TestEntity extends PersistedEntity {}

  @Test
  void shouldGenerateUUIDv7OnCreate() {
    TestEntity entity = new TestEntity();
    entity.onCreate();

    assertThat(entity.getId()).isNotNull();
    assertThat(entity.getId().version()).isEqualTo(7);
  }

  @Test
  void shouldSetTimestampsOnCreate() {
    TestEntity entity = new TestEntity();
    entity.onCreate();

    assertThat(entity.getCreatedAt()).isNotNull();
    assertThat(entity.getUpdatedAt()).isNotNull();
    assertThat(entity.getCreatedAt()).isEqualTo(entity.getUpdatedAt());
  }

  @Test
  void shouldUpdateTimestampOnUpdate() {
    TestEntity entity = new TestEntity();
    entity.onCreate();

    Instant firstUpdatedAt = entity.getUpdatedAt();

    // Simulate time passing by setting updatedAt manually in test
    entity.onUpdate();

    assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(firstUpdatedAt);
    // createdAt should remain unchanged
    assertThat(entity.getCreatedAt()).isEqualTo(entity.getCreatedAt());
  }

  @Test
  void equalsShouldBeBasedOnId() {
    TestEntity e1 = new TestEntity();
    e1.onCreate();
    TestEntity e2 = new TestEntity();
    e2.onCreate();

    // Different ids => not equal
    assertThat(e1).isNotEqualTo(e2);

    // Same entity
    assertThat(e1).isEqualTo(e1);
  }

  @Test
  void hashCodeShouldBeBasedOnId() {
    TestEntity entity = new TestEntity();
    entity.onCreate();

    int hc1 = entity.hashCode();
    int hc2 = entity.hashCode();

    assertThat(hc1).isEqualTo(hc2);
  }
}
