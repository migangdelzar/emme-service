package com.emme.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class NotificationApplicationBoundaryTest {
  private static final Path ROOT =
      sourcePath("modules/notification/src/main/java/com/emme/notification");
  private static final Path APPLICATION = ROOT.resolve("application");

  @Test
  void applicationOwnsPortsAndFocusedServices() {
    assertThat(Files.exists(APPLICATION.resolve("port/out/NotificationRepository.java"))).isTrue();
    assertThat(Files.exists(APPLICATION.resolve("port/out/EmailSender.java"))).isTrue();
    assertThat(Files.exists(APPLICATION.resolve("port/out/SmsSender.java"))).isTrue();
    assertThat(Files.exists(APPLICATION.resolve("port/out/PushSender.java"))).isTrue();
    assertThat(Files.exists(APPLICATION.resolve("service/RequestNotificationService.java")))
        .isTrue();
    assertThat(Files.exists(APPLICATION.resolve("service/DeliverNotificationService.java")))
        .isTrue();
    assertThat(Files.exists(APPLICATION.resolve("service/CancelNotificationService.java")))
        .isTrue();
    assertThat(Files.exists(APPLICATION.resolve("service/GetNotificationService.java"))).isTrue();
    assertThat(Files.exists(APPLICATION.resolve("service/ListNotificationsService.java"))).isTrue();
  }

  @Test
  void applicationDoesNotDependOnPersistenceOrProviderAdapters() throws Exception {
    try (Stream<Path> paths = Files.walk(APPLICATION)) {
      for (Path path : paths.filter(candidate -> candidate.toString().endsWith(".java")).toList()) {
        assertThat(Files.readString(path))
            .as("application source: %s", path)
            .doesNotContain("com.emme.notification.adapter.out.persistence")
            .doesNotContain("com.emme.notification.adapter.out.provider")
            .doesNotContain("com.emme.notification.adapter.out.client")
            .doesNotContain("jakarta.persistence")
            .doesNotContain("org.springframework.data");
      }
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
