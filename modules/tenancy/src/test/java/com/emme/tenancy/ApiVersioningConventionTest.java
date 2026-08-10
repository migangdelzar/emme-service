package com.emme.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApiVersioningConventionTest {

  @Test
  void keepsInboundHttpRoutesVersionNeutralWhenApiVersionComesFromTheHeader() throws IOException {
    Path modules = sourcePath("modules");
    try (var paths = Files.walk(modules)) {
      List<Path> inboundAdapters =
          paths
              .filter(path -> path.toString().contains("/adapter/in/"))
              .filter(path -> path.toString().endsWith(".java"))
              .toList();

      for (Path adapter : inboundAdapters) {
        assertThat(Files.readString(adapter))
            .as("inbound adapter: %s", adapter)
            .doesNotContain("/api/v1");
      }
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
    throw new IllegalStateException("Cannot locate source path: " + relativePath);
  }
}
