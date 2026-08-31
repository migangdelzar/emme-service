package com.emme.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AiDesignImageMigrationContractTest {
  @Test
  void definesTenantScopedDurableImageMetadata() throws IOException {
    String sql = resource("db/emme-studio/releases/0.1.0/027-ai-design-images.sql");
    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS ai_design_image")
        .contains("tenant_id UUID NOT NULL")
        .contains("storage_key VARCHAR(1000) NOT NULL")
        .contains("ALTER TABLE ai_design_image ENABLE ROW LEVEL SECURITY")
        .contains("tenant_id = current_tenant_id()")
        .contains("UNIQUE (tenant_id, workflow_id)");
  }

  @Test
  void isIncludedByChangelog() throws IOException {
    assertThat(resource("db/emme-studio/changelog.yaml"))
        .contains("releases/0.1.0/027-ai-design-images.sql");
  }

  private static String resource(String path) throws IOException {
    try (InputStream stream = AiDesignImageMigrationContractTest.class.getClassLoader().getResourceAsStream(path)) {
      if (stream == null) throw new IOException("Missing migration resource: " + path);
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
