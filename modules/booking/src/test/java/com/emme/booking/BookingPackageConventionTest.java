package com.emme.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BookingPackageConventionTest {

  private static final Path ROOT_PACKAGE_INFO =
      sourcePath("modules/booking/src/main/java/com/emme/booking/package-info.java");
  private static final Path API_PACKAGE_INFO =
      sourcePath("modules/booking/src/main/java/com/emme/booking/api/package-info.java");
  private static final Path MODULE_PACKAGE =
      sourcePath("modules/booking/src/main/java/com/emme/booking");
  private static final Path LEGACY_EVENTS_PACKAGE_INFO =
      sourcePath("modules/booking/src/main/java/com/emme/booking/events/package-info.java");

  private static Path sourcePath(String relativePath) {
    Path current = Path.of("").toAbsolutePath();
    for (int attempt = 0; attempt < 6 && current != null; attempt++) {
      Path candidate = current.resolve(relativePath);
      if (Files.exists(candidate)) {
        return candidate;
      }
      current = current.getParent();
    }
    return Path.of(relativePath);
  }

  @Test
  void keepsApplicationModuleMetadataAndOnlyActualDependencies() throws IOException {
    String source = Files.readString(ROOT_PACKAGE_INFO);

    assertThat(source).contains("@org.springframework.modulith.ApplicationModule");
    assertThat(source).contains("\"shared\"");
    assertThat(source).contains("\"tenancy\"");
    assertThat(source).doesNotContain(
        "studio :: studio-api",
        "customer :: customer-api",
        "workforce :: workforce-api",
        "catalog :: catalog-api");
  }

  @Test
  void doesNotExposeAnEmptyLegacyBookingApiNamedInterface() throws IOException {
    String source = Files.readString(API_PACKAGE_INFO);

    assertThat(source).doesNotContain("@org.springframework.modulith.NamedInterface");
    assertThat(source).contains("package com.emme.booking.api;");
  }

  @Test
  void doesNotKeepAnEmptyTopLevelEventsPackage() {
    assertThat(Files.exists(LEGACY_EVENTS_PACKAGE_INFO)).isFalse();
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
