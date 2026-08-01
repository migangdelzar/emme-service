package com.emme.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TenancyPackageConventionTest {

  private static final Path ROOT_PACKAGE_INFO =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/package-info.java");
  private static final Path API_PACKAGE_INFO =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/api/package-info.java");
  private static final Path USE_CASE_PACKAGE_INFO =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/api/usecase/package-info.java");
  private static final Path RESULT_PACKAGE_INFO =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/api/result/package-info.java");
  private static final Path LEGACY_API =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/api/TenantApi.java");
  private static final Path LEGACY_RESULT =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/api/TenantInfo.java");
  private static final Path EVENT =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/api/event/TenantCreated.java");
  private static final Path LEGACY_EVENT =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/api/event/TenantCreatedEvent.java");

  @Test
  void keepsModuleMetadataAtTheModuleRoot() throws IOException {
    String source = Files.readString(ROOT_PACKAGE_INFO);

    assertThat(source).contains("@org.springframework.modulith.ApplicationModule");
    assertThat(source).contains("\"shared\"");
  }

  @Test
  void groupsPublicContractsByKind() throws IOException {
    assertThat(Files.readString(API_PACKAGE_INFO))
        .doesNotContain("@org.springframework.modulith.NamedInterface");
    assertThat(Files.readString(USE_CASE_PACKAGE_INFO))
        .contains("@org.springframework.modulith.NamedInterface(\"tenant-api\")");
    assertThat(Files.readString(RESULT_PACKAGE_INFO))
        .contains("@org.springframework.modulith.NamedInterface(\"tenant-api\")");
  }

  @Test
  void removesLegacyUngroupedContractFilesAndUsesPastTenseEventNaming() {
    assertThat(Files.exists(LEGACY_API)).isFalse();
    assertThat(Files.exists(LEGACY_RESULT)).isFalse();
    assertThat(Files.exists(EVENT)).isTrue();
    assertThat(Files.exists(LEGACY_EVENT)).isFalse();
  }

  private static Path sourcePath(String relativePath) {
    Path current = Path.of("").toAbsolutePath();
    for (int attempt = 0; attempt < 8 && current != null; attempt++) {
      if (Files.exists(current.resolve("settings.gradle.kts"))) {
        return current.resolve(relativePath);
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate repository source: " + relativePath);
  }
}
