package com.emme.tenancy.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TestingFixtureBoundaryTest {

  private static final Path TESTING_FIXTURE_ROOT =
      sourcePath("libraries/testing/src/testFixtures/java/com/emme/testing");
  private static final Path TENANCY_FIXTURE_ROOT =
      sourcePath("modules/tenancy/src/testFixtures/java/com/emme/tenancy/testing");
  private static final Path TENANCY_BOOTSTRAP_FIXTURE =
      sourcePath(
          "modules/tenancy/src/testFixtures/java/com/emme/testing/TestBootstrapJdbcConfig.java");

  @Test
  void genericWebFixtureDoesNotOwnTenantProvisioning() throws Exception {
    assertThat(Files.readString(TESTING_FIXTURE_ROOT.resolve("BaseWebTest.java")))
        .doesNotContain("TestBootstrapJdbcConfig");
    assertThat(TESTING_FIXTURE_ROOT.resolve("BaseSpringModuleTest.java")).doesNotExist();
    assertThat(TESTING_FIXTURE_ROOT.resolve("TestBootstrapJdbcConfig.java")).doesNotExist();
    assertThat(TENANCY_FIXTURE_ROOT.resolve("BaseTenantModuleTest.java")).exists();
    assertThat(TENANCY_BOOTSTRAP_FIXTURE).exists();
  }

  private static Path sourcePath(String relativePath) {
    Path current = Path.of("").toAbsolutePath();
    for (int attempt = 0; attempt < 8 && current != null; attempt++) {
      if (Files.exists(current.resolve("settings.gradle.kts"))) {
        return current.resolve(relativePath);
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate repository source: " + relativePath);
  }
}
