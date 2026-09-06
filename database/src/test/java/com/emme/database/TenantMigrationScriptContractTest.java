package com.emme.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TenantMigrationScriptContractTest {

  private static final Path SCRIPT = Path.of("docker/run-migrations.sh");

  @Test
  void validatesTenantSlugsBeforeWritingProvisioningState() throws Exception {
    String script = Files.readString(SCRIPT);

    assertThat(script)
        .contains("[[ ! \"${slug}\" =~ ^[A-Za-z][A-Za-z0-9-]{0,62}$ ]]")
        .contains("echo \"Unsafe tenant slug: ${slug}\" >&2");
  }
}
