package com.emme;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Protects the deployable application contract while {@code studio-api} remains a compatibility
 * target.
 */
class PlatformApplicationParityTest {

  @Test
  void canonicalApplicationUsesTheMvpRuntimeContract() throws IOException {
    String applicationConfiguration = Files.readString(Path.of("src/main/resources/application.yml"));

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
        module -> assertThat(buildConfiguration).contains("implementation(project(\"" + module + "\"))"));
  }
}
