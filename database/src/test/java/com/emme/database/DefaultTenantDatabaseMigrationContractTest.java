package com.emme.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DefaultTenantDatabaseMigrationContractTest {

  @Test
  void assignsTheSharedDefaultDatabaseToTenantsWithoutRoutingMetadata() throws Exception {
    String changelog = Files.readString(Path.of("src/main/resources/db/emme-core/changelog.yaml"));
    String migration =
        Files.readString(
            Path.of(
                "src/main/resources/db/emme-core/releases/0.1.0/014-default-tenant-database.sql"));

    assertThat(changelog).contains("014-default-tenant-database.sql");
    assertThat(migration)
        .contains("UPDATE emme_core.tenant")
        .contains("SET database_id = '00000000-0000-0000-0000-000000000000'")
        .contains("WHERE database_id IS NULL");
  }
}
