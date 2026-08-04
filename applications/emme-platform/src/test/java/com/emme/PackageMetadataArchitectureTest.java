package com.emme;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.testing.architecture.PackageMetadataRules;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Verifies that every materialized production package explains its boundary metadata. */
class PackageMetadataArchitectureTest {

  @Test
  void everyMaterializedProductionPackageHasPackageInfo() {
    Path root = repositoryRoot();
    assertThat(
            PackageMetadataRules.packagesMissingMetadata(
                root.resolve("modules"),
                root.resolve("libraries"),
                root.resolve("applications/emme-platform/src/main/java")))
        .isEmpty();
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
