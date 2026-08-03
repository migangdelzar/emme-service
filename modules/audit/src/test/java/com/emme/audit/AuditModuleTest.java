package com.emme.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.testing.BaseUnitTest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AuditModuleTest extends BaseUnitTest {

  @Test
  void moduleLoads() {
    assertThat(getClass().getPackageName()).contains("audit");
  }

  @Test
  void keepsReservedBoundaryMetadataOnly() {
    Path root = sourcePath("modules/audit/src/main/java/com/emme/audit");
    String metadata = read(root.resolve("package-info.java"));

    assertThat(Files.exists(root.resolve("package-info.java"))).isTrue();
    assertThat(metadata).contains("allowedDependencies = {}");
    assertThat(Files.exists(root.resolve("api"))).isFalse();
    assertThat(Files.exists(root.resolve("domain"))).isFalse();
    assertThat(Files.exists(root.resolve("application"))).isFalse();
    assertThat(Files.exists(root.resolve("adapter"))).isFalse();
  }

  private static String read(Path path) {
    try {
      return Files.readString(path);
    } catch (java.io.IOException exception) {
      throw new IllegalStateException("Cannot read source path: " + path, exception);
    }
  }

  private static Path sourcePath(String relativePath) {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve(relativePath);
      if (Files.exists(candidate)) return candidate;
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate source path: " + relativePath);
  }
}
