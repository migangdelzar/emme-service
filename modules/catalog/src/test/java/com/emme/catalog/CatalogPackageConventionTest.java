package com.emme.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CatalogPackageConventionTest {

  private static final Path SOURCE_ROOT =
      sourcePath("modules/catalog/src/main/java/com/emme/catalog");

  @Test
  void everyMaterializedProductionPackageHasLocalMetadata() throws IOException {
    try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
      files
          .filter(Files::isDirectory)
          .filter(this::containsProductionJavaSource)
          .forEach(
              packageDirectory ->
                  assertThat(Files.exists(packageDirectory.resolve("package-info.java")))
                      .as("package metadata for %s", packageDirectory)
                      .isTrue());
    }
  }

  @Test
  void catalogMatchingDependsOnACatalogOwnedSearchPort() throws IOException {
    String serviceSource =
        Files.readString(SOURCE_ROOT.resolve("application/service/CatalogMatchService.java"));

    assertThat(serviceSource).contains("CatalogSearchPort");
    assertThat(serviceSource).doesNotContain("com.emme.shared.search.HybridSearch");
    assertThat(serviceSource).doesNotContain("com.emme.shared.search.SearchTarget");
    assertThat(Files.exists(SOURCE_ROOT.resolve("application/port/out/CatalogSearchPort.java")))
        .isTrue();
    assertThat(
            Files.exists(
                SOURCE_ROOT.resolve("adapter/out/client/search/HybridCatalogSearchAdapter.java")))
        .isTrue();
  }

  @Test
  void catalogApiRemainsTheOnlyPublicNamedInterface() throws IOException {
    String apiMetadata = Files.readString(SOURCE_ROOT.resolve("api/package-info.java"));

    assertThat(apiMetadata)
        .contains("@org.springframework.modulith.NamedInterface(\"catalog-api\")");
  }

  private boolean containsProductionJavaSource(Path directory) {
    try (Stream<Path> files = Files.list(directory)) {
      return files.anyMatch(
          file ->
              file.getFileName().toString().endsWith(".java")
                  && !file.getFileName().toString().equals("package-info.java"));
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to inspect Catalog package " + directory, exception);
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
