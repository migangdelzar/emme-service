package com.emme.subscriptions.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SubscriptionProvisioningBoundaryTest {

  private static final Path LISTENER =
      Path.of(
          "src/main/java/com/emme/subscriptions/adapter/in/messaging/consumer/SubscriptionProvisioningListener.java");

  @Test
  void activationListenerDoesNotOwnBootstrapJdbcOrSchemaSql() throws Exception {
    String source = Files.readString(LISTENER);

    assertThat(source)
        .doesNotContain("JdbcTemplate")
        .doesNotContain("JdbcClient")
        .doesNotContain("bootstrapJdbc")
        .doesNotContain("schemaName()")
        .doesNotContain("jdbc.update");
  }
}
