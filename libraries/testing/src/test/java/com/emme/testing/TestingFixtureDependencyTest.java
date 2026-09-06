package com.emme.testing;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class TestingFixtureDependencyTest {

  private static final List<String> FEATURE_NAMES =
      List.of("com.emme.identity", "com.emme.salon", "com.emme.subscriptions", "com.emme.tenancy");

  @Test
  void genericFixturesDoNotReferenceFeaturePackagesOrProviderClients() throws IOException {
    Path fixtureRoot = sourcePath("libraries/testing/src/testFixtures");

    try (Stream<Path> sources = Files.walk(fixtureRoot)) {
      sources
          .filter(path -> path.toString().endsWith(".java"))
          .forEach(
              source -> {
                String contents = read(source);
                assertThat(contents)
                    .as("generic fixture source: %s", source)
                    .doesNotContain(FEATURE_NAMES.toArray(String[]::new))
                    .doesNotContain("KeycloakAdminClient")
                    .doesNotContain("com.google.api.client");
              });
    }
  }

  @Test
  void genericTestingBuildDoesNotDependOnFeatureModules() throws IOException {
    String build = Files.readString(sourcePath("libraries/testing/build.gradle.kts"));

    assertThat(build)
        .doesNotContain(":modules:identity")
        .doesNotContain(":modules:salon")
        .doesNotContain(":modules:subscriptions")
        .doesNotContain(":modules:tenancy")
        .doesNotContain("keycloak-admin-client");
  }

  private static String read(Path source) {
    try {
      return Files.readString(source);
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot read " + source, exception);
    }
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
    throw new IllegalStateException("Cannot locate repository source: " + relativePath);
  }
}
