package com.emme.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PaymentApplicationBoundaryTest {

  private static final Path ROOT = sourcePath("modules/payment/src/main/java/com/emme/payment");
  private static final Path APPLICATION = ROOT.resolve("application");

  @Test
  void applicationOwnsPortsAndFocusedServices() {
    assertThat(Files.exists(APPLICATION.resolve("port/out/PaymentRepository.java"))).isTrue();
    assertThat(Files.exists(APPLICATION.resolve("port/out/PaymentProvider.java"))).isTrue();
    assertThat(Files.exists(APPLICATION.resolve("service/InitiatePaymentService.java"))).isTrue();
    assertThat(Files.exists(APPLICATION.resolve("service/AuthorizePaymentService.java"))).isTrue();
    assertThat(Files.exists(APPLICATION.resolve("service/CapturePaymentService.java"))).isTrue();
    assertThat(Files.exists(APPLICATION.resolve("service/RefundPaymentService.java"))).isTrue();
    assertThat(Files.exists(APPLICATION.resolve("service/GetPaymentService.java"))).isTrue();
    assertThat(Files.exists(APPLICATION.resolve("service/ListPaymentsService.java"))).isTrue();
    assertThat(Files.exists(APPLICATION.resolve("service/ProcessPaymentCallbackService.java")))
        .isTrue();
  }

  @Test
  void applicationDoesNotDependOnAdaptersOrPersistenceFrameworks() throws IOException {
    try (Stream<Path> paths = Files.walk(APPLICATION)) {
      for (Path path : paths.filter(candidate -> candidate.toString().endsWith(".java")).toList()) {
        String source = Files.readString(path);
        assertThat(source)
            .as("application source: %s", path)
            .doesNotContain("com.emme.payment.adapter.out.persistence")
            .doesNotContain("com.emme.payment.adapter.out.client")
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
