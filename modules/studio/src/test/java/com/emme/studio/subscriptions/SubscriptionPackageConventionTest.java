package com.emme.studio.subscriptions;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SubscriptionPackageConventionTest {

  private static final Path ROOT =
      sourcePath("modules/studio/src/main/java/com/emme/studio/subscriptions");

  @Test
  void replacesLegacyPackagesWithCanonicalModuleBoundaries() {
    assertThat(hasJavaSources(ROOT.resolve("entity"))).isFalse();
    assertThat(hasJavaSources(ROOT.resolve("web"))).isFalse();
    assertThat(Files.exists(ROOT.resolve("domain/model/Subscription.java"))).isTrue();
    assertThat(Files.exists(ROOT.resolve("adapter/out/persistence/entity/SubscriptionEntity.java")))
        .isTrue();
    assertThat(Files.exists(ROOT.resolve("adapter/in/web/controller/SubscriptionController.java")))
        .isTrue();
    assertThat(Files.exists(ROOT.resolve("api/usecase/CreateSubscriptionUseCase.java"))).isTrue();
  }

  @Test
  void hasOneApplicationServicePerUseCase() throws Exception {
    Path services = ROOT.resolve("application/service");
    try (Stream<Path> files = Files.list(services)) {
      files
          .filter(path -> path.getFileName().toString().endsWith("Service.java"))
          .forEach(
              path -> {
                try {
                  assertThat(Files.readString(path).split("implements ", -1).length - 1)
                      .as("%s must implement one use case", path)
                      .isLessThanOrEqualTo(1);
                } catch (Exception exception) {
                  throw new IllegalStateException("Cannot inspect " + path, exception);
                }
              });
    }
  }

  @Test
  void inboundControllerRequiresCurrentTenantContext() throws Exception {
    String source =
        Files.readString(ROOT.resolve("adapter/in/web/controller/SubscriptionController.java"));
    assertThat(source).contains("withCurrentTenant");
  }

  private static boolean hasJavaSources(Path directory) {
    if (!Files.isDirectory(directory)) {
      return false;
    }
    try (Stream<Path> paths = Files.walk(directory)) {
      return paths.anyMatch(path -> path.toString().endsWith(".java"));
    } catch (Exception exception) {
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
