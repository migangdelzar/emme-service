package com.emme.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdGeneratorTest {

  @Test
  void shouldGenerateUUIDv7() {
    UUID id = IdGenerator.generate();

    assertThat(id).isNotNull();
    assertThat(id.version()).isEqualTo(7);
  }

  @Test
  void shouldGenerateUniqueIds() {
    Set<UUID> ids = new HashSet<>();
    for (int i = 0; i < 1000; i++) {
      ids.add(IdGenerator.generate());
    }
    assertThat(ids).hasSize(1000);
  }

  @Test
  void shouldGenerateTimeOrderedIds() throws InterruptedException {
    UUID first = IdGenerator.generate();
    Thread.sleep(10);
    UUID second = IdGenerator.generate();

    // UUIDv7 encodes timestamp in most-significant bits;
    // compare lexicographically (same effect for same-version UUIDs)
    assertThat(second.compareTo(first)).isGreaterThan(0);
  }
}
