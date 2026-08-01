package com.emme.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CustomerPackageConventionTest {

  private static final Path ROOT_PACKAGE_INFO =
      sourcePath("modules/customer/src/main/java/com/emme/customer/package-info.java");
  private static final Path API_PACKAGE_INFO =
      sourcePath("modules/customer/src/main/java/com/emme/customer/api/package-info.java");
  private static final Path MODULE_PACKAGE =
      sourcePath("modules/customer/src/main/java/com/emme/customer");

  private static Path sourcePath(String relativePath) {
    Path current = Path.of("").toAbsolutePath();
    for (int attempt = 0; attempt < 6 && current != null; attempt++) {
      Path candidate = current.resolve(relativePath);
      if (Files.exists(candidate)) {
        return candidate;
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate repository source: " + relativePath);
  }

  @Test
  void keepsApplicationModuleMetadataAtTheModuleRoot() throws IOException {
    String source = Files.readString(ROOT_PACKAGE_INFO);

    assertThat(source).contains("@org.springframework.modulith.ApplicationModule");
    assertThat(source).contains("\"shared\"");
    assertThat(source).contains("\"tenancy\"");
  }

  @Test
  void doesNotExposeAnEmptyLegacyCustomerApiNamedInterface() throws IOException {
    String source = Files.readString(API_PACKAGE_INFO);

    assertThat(source).doesNotContain("@org.springframework.modulith.NamedInterface");
    assertThat(source).contains("package com.emme.customer.api;");
  }

  @Test
  void keepsProductionTypesUnderGroupedApiOrOwnedLayers() throws IOException {
    assertThat(directJavaFiles(MODULE_PACKAGE)).containsExactly("package-info.java");
    assertThat(directJavaFiles(MODULE_PACKAGE.resolve("api"))).containsExactly("package-info.java");
  }

  private static java.util.List<String> directJavaFiles(Path directory) throws IOException {
    try (var files = Files.list(directory)) {
      return files
          .filter(path -> path.getFileName().toString().endsWith(".java"))
          .map(path -> path.getFileName().toString())
          .sorted()
          .toList();
    }
  }
}
