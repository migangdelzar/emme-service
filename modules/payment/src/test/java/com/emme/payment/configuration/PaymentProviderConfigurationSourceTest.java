package com.emme.payment.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentProviderConfigurationSourceTest {

  private static final List<String> PROVIDERS =
      List.of(
          "ConektaProvider.java",
          "MercadoPagoProvider.java",
          "PayPalProvider.java",
          "StripeProvider.java");

  @Test
  void paymentProvidersAndWebhooksDoNotReadProcessEnvironmentDirectly() throws IOException {
    Path root = sourcePath("modules/payment/src/main/java/com/emme/payment");
    Path providers = root.resolve("adapter/out/provider");

    for (String provider : PROVIDERS) {
      assertThat(Files.readString(providers.resolve(providerPackage(provider)).resolve(provider)))
          .as("provider source: %s", provider)
          .doesNotContain("System.getenv(");
    }

    assertThat(
            Files.readString(root.resolve("adapter/in/webhook/MercadoPagoWebhookController.java")))
        .doesNotContain("System.getenv(");
    assertThat(Files.exists(root.resolve("configuration/PaymentProperties.java"))).isTrue();
    assertThat(Files.exists(root.resolve("configuration/PaymentProviderConfiguration.java")))
        .isTrue();
    assertThat(Files.exists(root.resolve("configuration/PaymentProviderConfig.java"))).isFalse();
  }

  private static String providerPackage(String provider) {
    return switch (provider) {
      case "ConektaProvider.java" -> "conekta";
      case "MercadoPagoProvider.java" -> "mercadopago";
      case "PayPalProvider.java" -> "paypal";
      default -> "stripe";
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
