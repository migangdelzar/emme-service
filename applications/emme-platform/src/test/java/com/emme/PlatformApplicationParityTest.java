package com.emme;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Protects the sole deployable application contract. */
class PlatformApplicationParityTest {

  @Test
  void canonicalApplicationUsesTheMvpRuntimeContract() throws IOException {
    String applicationConfiguration =
        Files.readString(Path.of("src/main/resources/application.yml"));

    assertThat(applicationConfiguration)
        .contains("name: emme-platform")
        .contains("port: 8081")
        .contains("probes:")
        .contains("enabled: true")
        .contains("include: health,info,metrics,prometheus,env,loggers,modulith");
  }

  @Test
  void canonicalApplicationComposesThePlatformModules() throws IOException {
    String buildConfiguration = Files.readString(Path.of("build.gradle.kts"));
    List<String> requiredModules =
        List.of(
            ":modules:shared",
            ":modules:tenancy",
            ":modules:identity",
            ":modules:services",
            ":modules:clients",
            ":modules:appointments",
            ":modules:salon",
            ":modules:subscriptions",
            ":modules:documents",
            ":modules:staffing",
            ":modules:catalog",
            ":modules:booking",
            ":modules:calendar",
            ":modules:notification",
            ":modules:payment",
            ":modules:assistant",
            ":modules:audit");

    requiredModules.forEach(
        module ->
            assertThat(buildConfiguration).contains("implementation(project(\"" + module + "\"))"));
  }

  @Test
  void everyMaterializedApplicationConfigurationPackageHasLocalMetadata() throws IOException {
    Path configurationPackage =
        sourcePath("applications/emme-platform/src/main/java/com/emme/configuration");

    assertThat(configurationPackage.resolve("package-info.java")).exists();
  }

  @Test
  void ephemeralTestProfilesKeepSchemasAvailableDuringFrameworkShutdown() throws IOException {
    List<String> profiles =
        List.of(
            "applications/emme-platform/src/main/resources/application-test.yml",
            "applications/emme-platform/src/integrationTest/resources/application-kafka-test.yml",
            "libraries/testing/src/testFixtures/resources/application-test.yml",
            "libraries/testing/src/testFixtures/resources/application-repository.yml",
            "libraries/testing/src/testFixtures/resources/application-web.yml",
            "libraries/testing/src/testFixtures/resources/application-resttest.yml",
            "libraries/testing/src/testFixtures/resources/application-integration-test.yml");

    profiles.forEach(
        profile ->
            assertThat(readSource(profile))
                .as("ephemeral database profile: %s", profile)
                .contains("ddl-auto: create\n")
                .doesNotContain("ddl-auto: create-drop"));
  }

  @Test
  void ephemeralTestProfilesDisableDatabaseBackedScheduling() throws IOException {
    List<String> profiles =
        List.of(
            "applications/emme-platform/src/main/resources/application-test.yml",
            "applications/emme-platform/src/integrationTest/resources/application-kafka-test.yml",
            "libraries/testing/src/testFixtures/resources/application.yml",
            "libraries/testing/src/testFixtures/resources/application-test.yml",
            "libraries/testing/src/testFixtures/resources/application-repository.yml",
            "libraries/testing/src/testFixtures/resources/application-web.yml",
            "libraries/testing/src/testFixtures/resources/application-resttest.yml",
            "libraries/testing/src/testFixtures/resources/application-integration-test.yml");

    profiles.forEach(
        profile ->
            assertThat(readSource(profile))
                .as("ephemeral Spring profile: %s", profile)
                .contains("scheduling:\n      enabled: false")
                .contains("tenant:\n    provisioning:\n      enabled: false"));
  }

  private static Path sourcePath(String relativePath) {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve(relativePath);
      if (Files.exists(candidate)) {
        return candidate;
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate source path: " + relativePath);
  }

  private static String readSource(String relativePath) {
    try {
      return Files.readString(sourcePath(relativePath));
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot read source path: " + relativePath, exception);
    }
  }
}
