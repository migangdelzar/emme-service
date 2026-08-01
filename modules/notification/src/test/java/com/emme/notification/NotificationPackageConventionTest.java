package com.emme.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class NotificationPackageConventionTest {
  private static final Path ROOT =
      sourcePath("modules/notification/src/main/java/com/emme/notification");

  @Test
  void removesLegacyPackagesAndCreatesCanonicalBoundaries() {
    assertThat(hasJavaSources(ROOT.resolve("entity"))).isFalse();
    assertThat(hasJavaSources(ROOT.resolve("web"))).isFalse();
    assertThat(hasDirectJavaSources(ROOT.resolve("application"))).isFalse();
    assertThat(Files.exists(ROOT.resolve("domain/model/Notification.java"))).isTrue();
    assertThat(Files.exists(ROOT.resolve("adapter/out/persistence/entity/NotificationEntity.java")))
        .isTrue();
    assertThat(Files.exists(ROOT.resolve("adapter/in/web/controller/NotificationController.java")))
        .isTrue();
    assertThat(Files.exists(ROOT.resolve("api/usecase/RequestNotificationUseCase.java"))).isTrue();
  }

  private static boolean hasJavaSources(Path directory) {
    if (!Files.isDirectory(directory)) return false;
    try (Stream<Path> paths = Files.walk(directory)) {
      return paths.anyMatch(path -> path.toString().endsWith(".java"));
    } catch (Exception exception) {
      throw new IllegalStateException("Cannot inspect " + directory, exception);
    }
  }

  private static boolean hasDirectJavaSources(Path directory) {
    if (!Files.isDirectory(directory)) return false;
    try (Stream<Path> paths = Files.list(directory)) {
      return paths.anyMatch(path -> path.toString().endsWith(".java"));
    } catch (Exception exception) {
      throw new IllegalStateException("Cannot inspect " + directory, exception);
    }
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
