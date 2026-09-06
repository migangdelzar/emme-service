package com.emme;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Freezes the provider HTTP migration inventory without scanning generated output. */
class ExternalProviderHttpBoundaryArchitectureTest {

  private static final List<String> RETAINED_OKHTTP_LOCATIONS =
      List.of(
          "applications/emme-platform/src/e2eTest/java/com/emme/client/UserSession.java",
          "modules/ai-platform/src/test/java/com/emme/ai/platform/configuration/AiProviderConfigurationIntegrationTest.java",
          "modules/notification/src/test/java/com/emme/notification/adapter/out/provider/TwilioSmsProviderContractTest.java",
          "modules/payment/src/test/java/com/emme/payment/adapter/out/provider/StripeProviderContractTest.java");

  private static final Set<String> PROVIDER_PRODUCTION_ALLOWLIST = Set.of();

  @Test
  void retainedOkHttpLocationsAreExplicitAndExist() {
    RETAINED_OKHTTP_LOCATIONS.forEach(
        relativePath -> assertThat(Files.exists(sourcePath(relativePath))).isTrue());
  }

  @Test
  void obsoleteProviderWrappersAreDeleted() {
    Path repository = sourcePath("settings.gradle.kts").getParent();
    assertThat(
            Files.exists(
                repository.resolve(
                    "modules/calendar/src/main/java/com/emme/calendar/configuration/GoogleHttpClient.java")))
        .isFalse();
    assertThat(
            Files.exists(
                repository.resolve(
                    "modules/notification/src/main/java/com/emme/notification/configuration/NotificationHttpClient.java")))
        .isFalse();
    assertThat(
            Files.exists(
                repository.resolve(
                    "modules/payment/src/main/java/com/emme/payment/configuration/PaymentHttpClient.java")))
        .isFalse();
  }

  @Test
  void ordinaryProductionProviderSourcesContainNoOkHttpReferences() throws IOException {
    Path repository = sourcePath("settings.gradle.kts").getParent();
    Set<String> productionReferences = new LinkedHashSet<>();

    try (Stream<Path> paths = Files.walk(repository)) {
      paths
          .filter(this::isStableProductionSource)
          .filter(this::containsOkHttpReference)
          .map(repository::relativize)
          .map(Path::toString)
          .forEach(productionReferences::add);
    }

    assertThat(productionReferences)
        .as("every production OkHttp reference must have an ordered provider migration task")
        .containsExactlyInAnyOrderElementsOf(PROVIDER_PRODUCTION_ALLOWLIST);
  }

  @Test
  void migrationLedgerDocumentsTheProviderBoundaryAndRetainedTransportSplit() throws IOException {
    String ledger =
        Files.readString(
            sourcePath("docs/superpowers/migrations/framework-first-migration-ledger.md"));

    assertThat(ledger)
        .contains("Provider HTTP transport policy")
        .contains("MockRestServiceServer")
        .contains("MockWebServer")
        .contains("UserSession")
        .contains("Notification transport")
        .contains("Payment transport")
        .contains("Google transport")
        .contains("Keycloak transport");
  }

  private boolean isStableProductionSource(Path path) {
    String normalized = path.toString().replace('\\', '/');
    return normalized.endsWith(".java")
        && normalized.contains("/src/main/")
        && !normalized.contains("/build/");
  }

  private boolean containsOkHttpReference(Path path) {
    try {
      String source = Files.readString(path);
      return source.contains("import okhttp3") || source.contains("OkHttpClient");
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot inspect " + path, exception);
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
