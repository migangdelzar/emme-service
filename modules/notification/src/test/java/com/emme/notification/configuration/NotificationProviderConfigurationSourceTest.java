package com.emme.notification.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationProviderConfigurationSourceTest {

  private static final List<String> PROVIDERS =
      List.of(
          "ApnsPushProvider.java",
          "FcmPushProvider.java",
          "MessageBirdProvider.java",
          "SendGridProvider.java",
          "SesEmailProvider.java",
          "SmtpEmailProvider.java",
          "TwilioSmsProvider.java",
          "VonageProvider.java");

  @Test
  void productionProvidersDoNotReadProcessEnvironmentDirectly() throws IOException {
    Path providerRoot =
        sourcePath("modules/notification/src/main/java/com/emme/notification/adapter/out/provider");

    for (String provider : PROVIDERS) {
      assertThat(
              Files.readString(providerRoot.resolve(providerPackage(provider)).resolve(provider)))
          .as("provider source: %s", provider)
          .doesNotContain("System.getenv(");
    }

    assertThat(
            Files.exists(
                sourcePath(
                    "modules/notification/src/main/java/com/emme/notification/configuration/NotificationProperties.java")))
        .isTrue();
  }

  private static String providerPackage(String provider) {
    return switch (provider) {
      case "ApnsPushProvider.java", "FcmPushProvider.java", "MockPushProvider.java" -> "push";
      case "MessageBirdProvider.java",
          "MockSmsProvider.java",
          "TwilioSmsProvider.java",
          "VonageProvider.java" ->
          "sms";
      default -> "email";
    };
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
