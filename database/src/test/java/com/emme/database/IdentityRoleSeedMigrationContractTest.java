package com.emme.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class IdentityRoleSeedMigrationContractTest {

  @Test
  void seedsStableTenantRolesIdempotentlyInTheCoreSchema() throws Exception {
    String changelog = Files.readString(Path.of("src/main/resources/db/emme-core/changelog.yaml"));
    String migration =
        Files.readString(
            Path.of(
                "src/main/resources/db/emme-core/releases/0.1.0/013-identity-tenant-roles.sql"));

    assertThat(changelog).contains("013-identity-tenant-roles.sql");
    assertThat(migration)
        .contains("INSERT INTO emme_core.role")
        .contains("tenant_owner")
        .contains("tenant_staff")
        .contains("ON CONFLICT (code) DO NOTHING");
  }
}
