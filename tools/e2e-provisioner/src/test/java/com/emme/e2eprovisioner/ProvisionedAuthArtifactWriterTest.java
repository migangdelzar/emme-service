package com.emme.e2eprovisioner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProvisionedAuthArtifactWriterTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void writesOneCredentialArtifactPerTenant(@TempDir Path outputDirectory) throws Exception {
    var writer = new ProvisionedAuthArtifactWriter(objectMapper, outputDirectory);

    var artifact =
        writer.write(
            "e2e-studio",
            Map.of(
                "admin", new ProvisionedAuthArtifactWriter.Credentials("admin", "admin-pass"),
                "owner", new ProvisionedAuthArtifactWriter.Credentials("owner", "owner-pass")));

    assertThat(artifact).isEqualTo(outputDirectory.resolve("e2e-studio.json"));
    JsonNode document = objectMapper.readTree(Files.readString(artifact));
    assertThat(document.path("version").asInt()).isEqualTo(1);
    assertThat(document.path("tenantSlug").asText()).isEqualTo("e2e-studio");
    assertThat(document.path("users").path("admin").path("credentials").path("username").asText())
        .isEqualTo("admin");
    assertThat(document.path("users").path("owner").path("credentials").path("password").asText())
        .isEqualTo("owner-pass");
    assertThat(document.path("users").path("owner").path("storageState").path("cookies").isArray())
        .isTrue();
    assertThat(document.path("users").path("owner").path("storageState").path("origins").isArray())
        .isTrue();
  }

  @Test
  void rejectsPathTraversalInTenantSlug(@TempDir Path outputDirectory) {
    var writer = new ProvisionedAuthArtifactWriter(objectMapper, outputDirectory);

    assertThatThrownBy(
            () ->
                writer.write(
                    "../outside",
                    Map.of(
                        "owner", new ProvisionedAuthArtifactWriter.Credentials("owner", "pass"))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("tenant slug");
  }
}
