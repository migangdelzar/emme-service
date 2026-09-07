package com.emme.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.TestApplication;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.payment.application.port.out.PaymentRepository;
import com.emme.payment.application.port.out.PaymentWebhookEventRepository;
import com.emme.payment.domain.model.Payment;
import com.emme.tenancy.adapter.out.client.database.TenantSchemaName;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = TestApplication.class)
@PostgresIntegrationTest
@DisplayName("payment integration test")
class PaymentIntegrationTest {

  @Autowired private DataSource dataSource;

  @Autowired private PaymentWebhookEventRepository webhookEvents;

  @Autowired private PaymentRepository payments;
  @Autowired private TenantProvisioningRepository provisioningRepository;
  @Autowired private TenantSchemaMigrationPort schemaMigrationPort;

  @Autowired private PlatformTransactionManager transactionManager;

  @Autowired
  @Qualifier("bootstrapJdbcClient")
  private JdbcClient bootstrapJdbc;

  @BeforeEach
  void installDatabaseExtensions() {
    bootstrapJdbc.sql("CREATE EXTENSION IF NOT EXISTS vector").update();
  }

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

  @Test
  @DisplayName("JPA payment persistence uses the tenant selected schema")
  void paymentCrudUsesTheTenantSelectedSchema() {
    UUID tenantA = provisionTenant("payment-a");
    UUID tenantB = provisionTenant("payment-b");
    Payment saved =
        TenantContextHolder.withTenantOverride(
            tenantA,
            () ->
                payments.save(
                    new Payment(
                        tenantA, "payment-" + UUID.randomUUID(), new BigDecimal("12.50"), "MXN")));
    UUID paymentId = saved.id();

    TenantContextHolder.withTenantOverride(
        tenantB, () -> assertThat(payments.findById(paymentId)).isEmpty());
    TenantContextHolder.withTenantOverride(
        tenantA,
        () ->
            assertThat(payments.findById(paymentId))
                .get()
                .extracting(Payment::tenantId)
                .isEqualTo(tenantA));
  }

  @Test
  @DisplayName("JPA payment updates reject a stale version")
  void concurrentPaymentUpdatesHaveOneOptimisticLockWinner() throws Exception {
    UUID tenantId = provisionTenant("payment-lock");
    UUID paymentId =
        TenantContextHolder.withTenantOverride(
            tenantId,
            () ->
                new TransactionTemplate(transactionManager)
                    .execute(
                        status ->
                            payments
                                .save(
                                    new Payment(
                                        tenantId,
                                        "payment-" + UUID.randomUUID(),
                                        new BigDecimal("12.50"),
                                        "MXN"))
                                .id()));
    CountDownLatch bothLoaded = new CountDownLatch(2);
    CountDownLatch releaseWrites = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<Throwable> first =
          executor.submit(() -> updatePayment(tenantId, paymentId, bothLoaded, releaseWrites));
      Future<Throwable> second =
          executor.submit(() -> updatePayment(tenantId, paymentId, bothLoaded, releaseWrites));

      assertThat(bothLoaded.await(10, TimeUnit.SECONDS)).isTrue();
      releaseWrites.countDown();

      Throwable firstFailure = first.get(10, TimeUnit.SECONDS);
      Throwable secondFailure = second.get(10, TimeUnit.SECONDS);
      assertThat(firstFailure == null ^ secondFailure == null).isTrue();
      assertThat(firstFailure == null ? secondFailure : firstFailure)
          .isInstanceOf(OptimisticLockingFailureException.class);
    } finally {
      releaseWrites.countDown();
      executor.shutdownNow();
    }
  }

  private Throwable updatePayment(
      UUID tenantId, UUID paymentId, CountDownLatch bothLoaded, CountDownLatch releaseWrites) {
    try {
      TenantContextHolder.withTenantOverride(
          tenantId,
          () ->
              new TransactionTemplate(transactionManager)
                  .executeWithoutResult(
                      status -> {
                        Payment payment = payments.findById(paymentId).orElseThrow();
                        bothLoaded.countDown();
                        await(releaseWrites);
                        payment.authorize();
                        payments.save(payment);
                      }));
      return null;
    } catch (Throwable failure) {
      return failure;
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timed out waiting for concurrent payment updates");
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(
          "Interrupted while coordinating concurrent payment updates", interrupted);
    }
  }

  private UUID provisionTenant(String prefix) {
    UUID tenantId = UUID.randomUUID();
    String slug = prefix + "-" + tenantId.toString().replace('-', 'a');
    String schemaName = TenantSchemaName.fromSlug(slug);
    provisioningRepository.requestProvisioning(tenantId, slug, schemaName);
    assertThat(schemaMigrationPort.migrate(tenantId, slug)).isEqualTo(schemaName);
    return tenantId;
  }
}
