package com.emme.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SharedOwnershipConventionTest {
  @Test
  void documentsTechnicalCapabilityOwnershipWithoutBusinessLayers() {
    Path root = sourcePath("modules/shared/src/main/java/com/emme/shared");
    assertThat(Files.exists(root.resolve("persistence/PersistedEntity.java"))).isTrue();
    assertThat(Files.exists(root.resolve("persistence/TenantOwnedEntity.java"))).isTrue();
    assertThat(Files.exists(root.resolve("time/ClockProvider.java"))).isTrue();
    assertThat(Files.exists(root.resolve("identity/IdGenerator.java"))).isTrue();
    assertThat(Files.exists(root.resolve("BaseEntity.java"))).isFalse();
    assertThat(Files.exists(root.resolve("TenantOwnedEntity.java"))).isFalse();
    assertThat(Files.exists(root.resolve("ClockProvider.java"))).isFalse();
    assertThat(Files.exists(root.resolve("IdGenerator.java"))).isFalse();
    assertThat(Files.exists(root.resolve("search/HybridSearch.java"))).isTrue();
    assertThat(Files.exists(root.resolve("web/advice/GlobalExceptionHandler.java"))).isTrue();
    assertThat(Files.exists(root.resolve("persistence/package-info.java"))).isTrue();
    assertThat(Files.exists(root.resolve("persistence/jdbc/package-info.java"))).isTrue();
    assertThat(Files.exists(root.resolve("time/package-info.java"))).isTrue();
    assertThat(Files.exists(root.resolve("identity/package-info.java"))).isTrue();
    assertThat(Files.exists(root.resolve("domain"))).isFalse();
    assertThat(Files.exists(root.resolve("application"))).isFalse();
    assertThat(Files.exists(root.resolve("adapter"))).isFalse();
  }

  @Test
  void everyEmbeddingMutationAndMaintenanceQueryRequiresTenantScope() throws Exception {
    Path source =
        sourcePath("modules/shared/src/main/java/com/emme/shared/search/HybridSearch.java");
    String contents = Files.readString(source);
    assertThat(contents).contains("WHERE tenant_id = :tenantId AND id = :id");
    assertThat(contents).contains("WHERE tenant_id = :tenantId AND embedding IS NULL");
    assertThat(contents).contains("count(*) FROM %s WHERE tenant_id = :tenantId");
  }

  @Test
  void exposesCapabilityPackagesThroughNamedInterfaces() throws Exception {
    Path root = sourcePath("modules/shared/src/main/java/com/emme/shared");
    assertThat(Files.readString(root.resolve("persistence/package-info.java")))
        .contains("NamedInterface(\"persistence\")");
    assertThat(Files.readString(root.resolve("persistence/jdbc/package-info.java")))
        .contains("NamedInterface(\"persistence-jdbc\")");
    assertThat(Files.readString(root.resolve("time/package-info.java")))
        .contains("NamedInterface(\"time\")");
    assertThat(Files.readString(root.resolve("identity/package-info.java")))
        .contains("NamedInterface(\"identity\")");
  }

  private static Path sourcePath(String relativePath) {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve(relativePath);
      if (Files.exists(candidate)) return candidate;
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate source path: " + relativePath);
  }
}
