package com.emme.assistant.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class WhatsAppConfigurationSourceTest {

  @Test
  void webhookComponentsUseTypedConfigurationInsteadOfDirectEnvironmentAccess() throws IOException {
    Path root = sourcePath("modules/assistant/src/main/java/com/emme/assistant");

    assertThat(Files.readString(root.resolve("adapter/in/messaging/WhatsAppMessageService.java")))
        .doesNotContain("@Value(")
        .doesNotContain("System.getenv(")
        .doesNotContain("skipping signature verification");
    assertThat(Files.readString(root.resolve("adapter/in/webhook/WhatsAppWebhookController.java")))
        .doesNotContain("@Value(");
    assertThat(
            Files.exists(root.resolve("adapter/in/webhook/WhatsAppWebhookSignatureVerifier.java")))
        .isTrue();
    assertThat(
            Files.readString(
                root.resolve("adapter/in/webhook/WhatsAppWebhookSignatureVerifier.java")))
        .contains("MessageDigest.isEqual");
    assertThat(Files.exists(root.resolve("configuration/WhatsAppProperties.java"))).isTrue();
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
