package com.emme.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class IdentityPackageConventionTest {

  private static final Path ROOT_PACKAGE_INFO =
      sourcePath("modules/identity/src/main/java/com/emme/identity/package-info.java");
  private static final Path API_PACKAGE_INFO =
      sourcePath("modules/identity/src/main/java/com/emme/identity/api/package-info.java");
  private static final Path USE_CASE_PACKAGE_INFO =
      sourcePath("modules/identity/src/main/java/com/emme/identity/api/usecase/package-info.java");
  private static final Path RESULT_PACKAGE_INFO =
      sourcePath("modules/identity/src/main/java/com/emme/identity/api/result/package-info.java");
  private static final Path LEGACY_API =
      sourcePath("modules/identity/src/main/java/com/emme/identity/api/IdentityApi.java");
  private static final Path LEGACY_MEMBERSHIP_RESULT =
      sourcePath("modules/identity/src/main/java/com/emme/identity/api/MembershipInfo.java");
  private static final Path LEGACY_USER_RESULT =
      sourcePath("modules/identity/src/main/java/com/emme/identity/api/UserInfo.java");

  @Test
  void keepsModuleMetadataAndExplicitAllowedDependencies() throws IOException {
    String source = Files.readString(ROOT_PACKAGE_INFO);

    assertThat(source).contains("@org.springframework.modulith.ApplicationModule");
    assertThat(source).contains("\"tenancy :: tenant-api\"");
    assertThat(source).contains("\"tenancy :: tenant-events\"");
  }

  @Test
  void groupsPublicContractsByKind() throws IOException {
    assertThat(Files.readString(API_PACKAGE_INFO))
        .doesNotContain("@org.springframework.modulith.NamedInterface");
    assertThat(Files.readString(USE_CASE_PACKAGE_INFO))
        .contains("@org.springframework.modulith.NamedInterface(\"identity-api\")");
    assertThat(Files.readString(RESULT_PACKAGE_INFO))
        .contains("@org.springframework.modulith.NamedInterface(\"identity-api\")");
  }

  @Test
  void removesLegacyUngroupedContractFiles() {
    assertThat(Files.exists(LEGACY_API)).isFalse();
    assertThat(Files.exists(LEGACY_MEMBERSHIP_RESULT)).isFalse();
    assertThat(Files.exists(LEGACY_USER_RESULT)).isFalse();
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
