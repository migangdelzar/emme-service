package com.emme;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTest {

  private static final Set<String> EXPECTED_MODULES =
      Set.of(
          "assistant",
          "audit",
          "booking",
          "calendar",
          "catalog",
          "customer",
          "identity",
          "notification",
          "payment",
          "shared",
          "studio",
          "tenancy",
          "workforce");

  static final ApplicationModules modules = ApplicationModules.of(EmmeApplication.class);

  @Test
  void generateModuleDocumentation() throws IOException {
    new Documenter(modules).writeDocumentation().writeIndividualModulesAsPlantUml();

    Path output = repositoryRoot().resolve("applications/emme-platform/build/spring-modulith-docs");
    assertThat(output.resolve("all-docs.adoc")).isRegularFile();
    assertThat(output.resolve("components.puml")).isRegularFile();
    for (String module : EXPECTED_MODULES) {
      assertThat(output.resolve("module-" + module + ".adoc")).isRegularFile();
      assertThat(output.resolve("module-" + module + ".puml")).isRegularFile();
    }
    assertThat(Files.readString(output.resolve("all-docs.adoc")))
        .contains("Identity", "Tenancy", "Studio");
  }

  @Test
  void moduleStructureIsValid() {
    modules.verify();
  }

  private static Path repositoryRoot() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      if (Files.isDirectory(current.resolve("modules"))
          && Files.isDirectory(current.resolve("applications/emme-platform"))) {
        return current;
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate repository root");
  }
}
