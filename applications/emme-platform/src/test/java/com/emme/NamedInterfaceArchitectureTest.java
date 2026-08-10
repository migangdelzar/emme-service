package com.emme;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.testing.architecture.NamedInterfaceRules;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Verifies that named interfaces are explicit, non-empty, and canonically named. */
class NamedInterfaceArchitectureTest {

  @Test
  void namedInterfacesAreExplicitAndNonEmpty() throws IOException {
    assertThat(NamedInterfaceRules.violations(sourcePath("modules"))).isEmpty();
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
