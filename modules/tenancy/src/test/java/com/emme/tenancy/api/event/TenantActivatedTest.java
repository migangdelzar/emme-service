package com.emme.tenancy.api.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantActivatedTest {

  @Test
  void rejectsASchemaNameThatCouldEscapeTheSubscriptionSchemaStatement() {
    assertThatThrownBy(
            () ->
                new TenantActivated(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "salon",
                    "safe_schema; DROP TABLE subscription",
                    "emme-salon"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid tenant schema name");
  }
}
