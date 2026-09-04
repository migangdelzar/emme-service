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
        Files.readString(SOURCE_ROOT.resolve("application/service/MatchCatalogItemsService.java"));

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

  @Test
  void catalogDoesNotDependOnAssistantForReusableAiCapabilities() throws IOException {
    String moduleMetadata = Files.readString(SOURCE_ROOT.resolve("package-info.java"));

    assertThat(moduleMetadata).doesNotContain("assistant");
  }

  @Test
  void catalogUsesFrameworkIndependentAiContractsInsteadOfProviderInternals() throws IOException {
    String imageService =
        Files.readString(
            SOURCE_ROOT.resolve("application/service/AddCatalogItemImageService.java"));
    String matchService =
        Files.readString(SOURCE_ROOT.resolve("application/service/MatchCatalogItemsService.java"));

    assertThat(imageService).contains("com.emme.ai.contracts.image.CaptionImageUseCase");
    assertThat(imageService).doesNotContain("com.emme.assistant.ai.application");
    assertThat(matchService).contains("com.emme.ai.contracts.image.CaptionImageUseCase");
    assertThat(matchService).contains("com.emme.ai.contracts.embedding.EmbeddingService");
    assertThat(matchService).doesNotContain("EmbedTextUseCase");
    assertThat(matchService).doesNotContain("com.emme.assistant.ai.application");
  }

  @Test
  void eachCatalogApplicationServiceImplementsAtMostOneUseCase() throws IOException {
    Path applicationServices = SOURCE_ROOT.resolve("application/service");
    try (Stream<Path> files = Files.walk(applicationServices)) {
      files
          .filter(path -> path.toString().endsWith("Service.java"))
          .forEach(
              path ->
                  assertThat(read(path))
                      .as("one use case per application service: %s", path)
                      .doesNotMatch("(?s).*implements\\s+[^\\{]*UseCase\\s*,.*"));
    }
  }

  @Test
  void imageStorageUsesTypedConfigurationInsteadOfValueInjection() throws IOException {
    assertThat(
            Files.readString(
                SOURCE_ROOT.resolve("adapter/out/client/storage/LocalImageStorage.java")))
        .doesNotContain("@Value(");
    assertThat(
            Files.exists(SOURCE_ROOT.resolve("configuration/CatalogImageStorageProperties.java")))
        .isTrue();
  }

  private static String read(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to read Catalog source " + path, exception);
    }
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
