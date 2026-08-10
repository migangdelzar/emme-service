package com.emme;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Public controller mappings use the header-based API version contract. */
class ApiVersionConventionTest {

  @Test
  void everyWebControllerDeclaresTheSupportedApiVersion() throws IOException {
    var modules = sourcePath("modules");

    try (var paths = Files.walk(modules)) {
      paths
          .filter(path -> path.toString().contains("/adapter/in/web/controller/"))
          .filter(path -> !path.toString().contains("/build/"))
          .filter(path -> path.toString().endsWith("Controller.java"))
          .forEach(
              path ->
                  assertThat(read(path))
                      .as("web controller: %s", path)
                      .contains("version = \"1.0\""));
    }
  }

  private static String read(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot read " + path, exception);
    }
  }

  private static Path sourcePath(String relativePath) {
    var current = Path.of("").toAbsolutePath();
    while (current != null) {
      var candidate = current.resolve(relativePath);
      if (Files.exists(candidate)) {
        return candidate;
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate source path: " + relativePath);
  }
}
