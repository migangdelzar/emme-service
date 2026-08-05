package com.emme.documents;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class DocumentUseCaseConventionTest {

  private static final Path ROOT =
      sourcePath("modules/documents/src/main/java/com/emme/documents");

  @Test
  void exposesGroupedContractsAndFocusedApplicationServices() throws Exception {
    assertThat(Files.exists(ROOT.resolve("api/command/UploadDocumentCommand.java"))).isTrue();
    assertThat(Files.exists(ROOT.resolve("api/query/GetDocumentQuery.java"))).isTrue();
    assertThat(Files.exists(ROOT.resolve("api/result/DocumentDetails.java"))).isTrue();
    assertThat(Files.exists(ROOT.resolve("api/usecase/UploadDocumentUseCase.java"))).isTrue();
    assertThat(Files.exists(ROOT.resolve("application/service/UploadDocumentService.java")))
        .isTrue();
    assertThat(Files.exists(ROOT.resolve("application/DocumentService.java"))).isFalse();
  }

  @Test
  void everyDocumentApplicationServiceImplementsAtMostOneUseCase() throws Exception {
    Path services = ROOT.resolve("application/service");
    try (Stream<Path> files = Files.list(services)) {
      files
          .filter(path -> path.getFileName().toString().endsWith("Service.java"))
          .forEach(
              path -> {
                try {
                  String source = Files.readString(path);
                  int implementedInterfaces = source.split("implements ", -1).length - 1;
                  assertThat(implementedInterfaces)
                      .as("%s must implement one use case", path)
                      .isLessThanOrEqualTo(1);
                } catch (Exception exception) {
                  throw new IllegalStateException("Cannot inspect " + path, exception);
                }
              });
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
