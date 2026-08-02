package com.emme.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PaymentPackageConventionTest {
  private static final Path ROOT = sourcePath("modules/payment/src/main/java/com/emme/payment");

  @Test
  void removesLegacyPackagesAndCreatesCanonicalBoundaries() {
    assertThat(hasJavaSources(ROOT.resolve("entity"))).isFalse();
    assertThat(hasJavaSources(ROOT.resolve("web"))).isFalse();
    assertThat(hasDirectJavaSources(ROOT.resolve("application"))).isFalse();
    assertThat(Files.exists(ROOT.resolve("domain/model/Payment.java"))).isTrue();
    assertThat(Files.exists(ROOT.resolve("adapter/out/persistence/entity/PaymentEntity.java")))
        .isTrue();
    assertThat(Files.exists(ROOT.resolve("adapter/in/web/controller/PaymentController.java")))
        .isTrue();
    assertThat(Files.exists(ROOT.resolve("adapter/in/webhook/MercadoPagoWebhookController.java")))
        .isTrue();
    assertThat(hasJavaSources(ROOT.resolve("adapter/in/web/controller"))).isTrue();
    assertThat(Files.exists(ROOT.resolve("api/usecase/InitiatePaymentUseCase.java"))).isTrue();
  }

  @Test
  void paymentMutationsAndReadsAreTenantScoped() throws Exception {
    String repository =
        Files.readString(ROOT.resolve("application/port/out/PaymentRepository.java"));
    String controller =
        Files.readString(ROOT.resolve("adapter/in/web/controller/PaymentController.java"));

    assertThat(repository).doesNotContain("findById(UUID paymentId)");
    assertThat(Files.readString(ROOT.resolve("api/query/GetPaymentQuery.java")))
        .contains("UUID tenantId");
    assertThat(Files.readString(ROOT.resolve("api/command/RefundPaymentCommand.java")))
        .contains("UUID tenantId");
    assertThat(Files.readString(ROOT.resolve("api/command/AuthorizePaymentCommand.java")))
        .contains("UUID tenantId");
    assertThat(Files.readString(ROOT.resolve("api/command/CapturePaymentCommand.java")))
        .contains("UUID tenantId");
    assertThat(controller).contains("withCurrentTenant");
  }

  @Test
  void paymentProvidersDoNotConstructTransportOrSerializationDependencies() throws Exception {
    try (Stream<Path> paths = Files.walk(ROOT.resolve("adapter/out/client"))) {
      paths
          .filter(path -> path.toString().endsWith("Provider.java"))
          .forEach(
              path -> {
                try {
                  String source = Files.readString(path);
                  assertThat(source)
                      .as("provider source %s", path)
                      .doesNotContain("new OkHttpClient(")
                      .doesNotContain("new ObjectMapper(");
                } catch (Exception exception) {
                  throw new IllegalStateException("Cannot inspect " + path, exception);
                }
              });
    }
    assertThat(Files.exists(ROOT.resolve("configuration/PaymentClientConfiguration.java"))).isTrue();
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
      return paths.anyMatch(
          path ->
              path.toString().endsWith(".java")
                  && !path.getFileName().toString().equals("package-info.java"));
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
