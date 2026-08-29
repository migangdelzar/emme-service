package com.emme.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AiAgeMigrationContractTest {

  private static final String MIGRATION = "db/emme-studio/releases/0.1.0/024-ai-age-graph.sql";

  @Test
  void definesAnOptionalTenantScopedDerivedGraphRegistry() throws IOException {
    String sql = resource(MIGRATION);

    assertThat(sql)
        .contains("CREATE TABLE IF NOT EXISTS ai_age_graph_registry")
        .contains("tenant_id UUID NOT NULL")
        .contains("graph_name VARCHAR(100) NOT NULL")
        .contains("projection_version BIGINT NOT NULL")
        .contains("ALTER TABLE ai_age_graph_registry ENABLE ROW LEVEL SECURITY")
        .contains("tenant_id = current_tenant_id()")
        .contains("pg_available_extensions")
        .contains("name = 'age'");
  }

  @Test
  void isIncludedByTheStudioChangelog() throws IOException {
    assertThat(resource("db/emme-studio/changelog.yaml"))
        .contains("releases/0.1.0/024-ai-age-graph.sql");
  }

  private static String resource(String name) throws IOException {
    try (InputStream stream =
        AiAgeMigrationContractTest.class.getClassLoader().getResourceAsStream(name)) {
      if (stream == null) {
        throw new IOException("Missing database migration resource: " + name);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
