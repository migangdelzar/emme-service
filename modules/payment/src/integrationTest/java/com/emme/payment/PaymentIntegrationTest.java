package com.emme.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.TestApplication;
import com.emme.payment.application.port.out.PaymentWebhookEventRepository;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestApplication.class)
@PostgresIntegrationTest
@DisplayName("payment integration test")
class PaymentIntegrationTest {

  @Autowired private DataSource dataSource;

  @Autowired private PaymentWebhookEventRepository webhookEvents;

  @Test
  @DisplayName("PostgreSQL container wired via @ServiceConnection")
  void postgresWired() throws Exception {
    try (var conn = dataSource.getConnection()) {
      var stmt = conn.createStatement();
      var rs = stmt.executeQuery("SELECT 1");
      assertThat(rs.next()).isTrue();
    }
  }

  @Test
  @DisplayName("Spring context boots")
  void contextLoads() {
    assertThat(dataSource).isNotNull();
  }

  @Test
  @DisplayName("PostgreSQL claim is tenant-scoped and replay-safe")
  void webhookClaimIsTenantScopedAndReplaySafe() {
    UUID tenantId = UUID.randomUUID();
    UUID otherTenantId = UUID.randomUUID();

    assertThat(webhookEvents.claim(tenantId, "stripe", "evt-integration-1")).isTrue();
    assertThat(webhookEvents.claim(tenantId, "stripe", "evt-integration-1")).isFalse();
    assertThat(webhookEvents.claim(otherTenantId, "stripe", "evt-integration-1")).isTrue();
  }
}
