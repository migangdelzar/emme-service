package com.emme.studio.documents;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DocumentsPackageConventionTest {

  private static final Path ROOT =
      sourcePath("modules/studio/src/main/java/com/emme/studio/documents");
  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.emme.studio.documents");

  @Test
  void removesLegacyImplementationPackages() {
    assertThat(hasJavaSources(ROOT.resolve("entity"))).isFalse();
    assertThat(hasJavaSources(ROOT.resolve("web"))).isFalse();
    assertThat(Files.exists(ROOT.resolve("domain/model/Document.java"))).isTrue();
    assertThat(Files.exists(ROOT.resolve("adapter/out/persistence/entity/DocumentEntity.java")))
        .isTrue();
    assertThat(Files.exists(ROOT.resolve("adapter/in/web/controller/DocumentController.java")))
        .isTrue();
  }

  @Test
  void keepsDomainAndApplicationIndependentFromFrameworkAndAdapters() throws IOException {
    assertThat(Files.readString(ROOT.resolve("domain/model/Document.java")))
        .doesNotContain("jakarta.persistence")
        .doesNotContain("org.springframework");
    assertThat(Files.readString(ROOT.resolve("application/DocumentService.java")))
        .doesNotContain("adapter.out")
        .doesNotContain("jakarta.persistence");
    noClasses()
        .that()
        .resideInAnyPackage(
            "com.emme.studio.documents.domain..", "com.emme.studio.documents.application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("com.emme.studio.documents.adapter.out..")
        .check(CLASSES);
  }

  private static boolean hasJavaSources(Path directory) {
    if (!Files.isDirectory(directory)) {
      return false;
    }
    try (var paths = Files.walk(directory)) {
      return paths.anyMatch(path -> path.toString().endsWith(".java"));
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot inspect " + directory, exception);
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
