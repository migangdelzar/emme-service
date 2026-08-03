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
            ":modules:studio",
            ":modules:customer",
            ":modules:workforce",
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
}
