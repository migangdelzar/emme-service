package com.emme.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.kernel.tracing.CorrelationId;
import org.junit.jupiter.api.Test;

class CorrelationIdTest {

  @Test
  void shouldSetAndRetrieveCorrelationId() {
    String id = CorrelationId.generate();
    CorrelationId.set(id);

    assertThat(CorrelationId.get()).isEqualTo(id);
  }

  @Test
  void shouldClearCorrelationId() {
    CorrelationId.set("test-123");
    CorrelationId.clear();

    assertThat(CorrelationId.get()).isNull();
  }

  @Test
  void shouldGenerateUniqueIds() {
    String id1 = CorrelationId.generate();
    String id2 = CorrelationId.generate();

    assertThat(id1).isNotEqualTo(id2);
  }
}
