package com.emme.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AppointmentCollisionMigrationContractTest {

  private static final String MIGRATION =
      "db/emme-studio/releases/0.1.0/033-appointment-overlap-constraint.sql";

  @Test
  void preventsOverlappingActiveAppointmentsPerTenantAndArtist() throws IOException {
    String sql = resource(MIGRATION);

    assertThat(sql)
        .contains("CREATE EXTENSION IF NOT EXISTS btree_gist")
        .contains("EXCLUDE USING gist")
        .contains("tenant_id WITH =")
        .contains("artist_id WITH =")
        .contains("tstzrange(starts_at, ends_at, '[)') WITH &&")
        .contains("status IN ('CONFIRMED', 'IN_PROGRESS')");
  }

  @Test
  void failsBeforeConstraintCreationWhenExistingActiveAppointmentsOverlap() throws IOException {
    assertThat(resource(MIGRATION))
        .contains("existing active appointment overlaps prevent collision constraint creation")
        .contains("RAISE EXCEPTION");
  }

  @Test
  void isIncludedByTheStudioChangelog() throws IOException {
    assertThat(resource("db/emme-studio/changelog.yaml"))
        .contains("releases/0.1.0/033-appointment-overlap-constraint.sql");
  }

  private static String resource(String path) throws IOException {
    try (InputStream stream =
        AppointmentCollisionMigrationContractTest.class
            .getClassLoader()
            .getResourceAsStream(path)) {
      assertThat(stream).as("classpath resource %s", path).isNotNull();
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
