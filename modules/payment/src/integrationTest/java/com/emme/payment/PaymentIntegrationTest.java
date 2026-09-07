package com.emme.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.TestApplication;
import com.emme.ai.contracts.appointment.AppointmentHold;
import com.emme.ai.contracts.payment.PaymentLink;
import com.emme.appointments.application.port.out.AppointmentHoldRepository;
import com.emme.appointments.application.port.out.AppointmentRepository;
import com.emme.appointments.domain.model.Appointment;
import com.emme.clients.application.port.out.CustomerRepository;
import com.emme.clients.domain.model.Customer;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.payment.api.port.out.PaymentWorkflowCorrelationRepository;
import com.emme.payment.application.port.out.PaymentLinkRepository;
import com.emme.payment.application.port.out.PaymentRepository;
import com.emme.payment.application.port.out.PaymentWebhookEventRepository;
import com.emme.payment.domain.model.Payment;
import com.emme.services.application.port.out.ArtistRepository;
import com.emme.services.application.port.out.ServiceRepository;
import com.emme.services.domain.model.Artist;
import com.emme.services.domain.model.Service;
import com.emme.tenancy.adapter.out.client.database.TenantSchemaName;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
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

  @Autowired private AppointmentHoldRepository appointmentHolds;
  @Autowired private AppointmentRepository appointments;
  @Autowired private CustomerRepository customers;
  @Autowired private ArtistRepository artists;
  @Autowired private ServiceRepository services;
  @Autowired private PaymentWorkflowCorrelationRepository correlations;
  @Autowired private PaymentLinkRepository paymentLinks;
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
    UUID tenantId = provisionTenant("payment-webhook-a");
    UUID otherTenantId = provisionTenant("payment-webhook-b");

    assertThat(
            TenantContextHolder.withTenantOverride(
                tenantId, () -> webhookEvents.claim(tenantId, "stripe", "evt-integration-1")))
        .isTrue();
    assertThat(
            TenantContextHolder.withTenantOverride(
                tenantId, () -> webhookEvents.claim(tenantId, "stripe", "evt-integration-1")))
        .isFalse();
    assertThat(
            TenantContextHolder.withTenantOverride(
                otherTenantId,
                () -> webhookEvents.claim(otherTenantId, "stripe", "evt-integration-1")))
        .isTrue();
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
  @DisplayName("Payment-link idempotency keys are scoped to the tenant schema")
  void paymentLinkIdempotencyIsScopedToTheTenantSchema() {
    UUID tenantA = provisionTenant("payment-link-a");
    UUID tenantB = provisionTenant("payment-link-b");
    String idempotencyKey = "payment-retry-" + UUID.randomUUID();
    PaymentLink linkA = paymentLink("https://payments.test/a", "2031-01-01T10:00:00Z");
    PaymentLink linkB = paymentLink("https://payments.test/b", "2031-01-01T11:00:00Z");

    TenantContextHolder.withTenantOverride(tenantA, () -> paymentLinks.save(linkA, idempotencyKey));
    TenantContextHolder.withTenantOverride(tenantB, () -> paymentLinks.save(linkB, idempotencyKey));

    TenantContextHolder.withTenantOverride(
        tenantA,
        () -> assertThat(paymentLinks.findByIdempotencyKey(idempotencyKey)).contains(linkA));
    TenantContextHolder.withTenantOverride(
        tenantB,
        () -> assertThat(paymentLinks.findByIdempotencyKey(idempotencyKey)).contains(linkB));
  }

  @Test
  @DisplayName("Payment workflow provider references are scoped to the tenant schema")
  void paymentWorkflowCorrelationIsScopedToTheTenantSchema() {
    UUID tenantA = provisionTenant("payment-correlation-a");
    UUID tenantB = provisionTenant("payment-correlation-b");
    AppointmentHold holdA = createHold(tenantA);
    AppointmentHold holdB = createHold(tenantB);
    String providerReference = "provider-reference-" + UUID.randomUUID();
    PaymentWorkflowCorrelationRepository.PaymentWorkflowCorrelation correlationA =
        new PaymentWorkflowCorrelationRepository.PaymentWorkflowCorrelation(
            UUID.randomUUID(), "test-provider", providerReference, holdA.holdId());
    PaymentWorkflowCorrelationRepository.PaymentWorkflowCorrelation correlationB =
        new PaymentWorkflowCorrelationRepository.PaymentWorkflowCorrelation(
            UUID.randomUUID(), "test-provider", providerReference, holdB.holdId());

    TenantContextHolder.withTenantOverride(tenantA, () -> correlations.save(correlationA));
    TenantContextHolder.withTenantOverride(tenantB, () -> correlations.save(correlationB));

    TenantContextHolder.withTenantOverride(
        tenantA,
        () -> {
          assertThat(
                  correlations.findByProviderAndProviderReference(
                      "test-provider", providerReference))
              .contains(correlationA);
          assertThat(correlations.findByWorkflowId(correlationB.workflowId())).isEmpty();
        });
    TenantContextHolder.withTenantOverride(
        tenantB,
        () -> {
          assertThat(
                  correlations.findByProviderAndProviderReference(
                      "test-provider", providerReference))
              .contains(correlationB);
          assertThat(correlations.findByWorkflowId(correlationA.workflowId())).isEmpty();
        });
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

  private static PaymentLink paymentLink(String checkoutUrl, String expiresAt) {
    return new PaymentLink(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "test-provider",
        checkoutUrl,
        java.time.Instant.parse(expiresAt));
  }

  private AppointmentHold createHold(UUID tenantId) {
    return TenantContextHolder.withTenantOverride(
        tenantId,
        () -> {
          UUID customerId = customers.save(new Customer(tenantId, "Payment customer")).getId();
          UUID serviceId =
              services
                  .save(
                      new Service(
                          tenantId,
                          "PAY-" + UUID.randomUUID().toString().substring(0, 8),
                          "Payment service",
                          60,
                          BigDecimal.TEN))
                  .getId();
          UUID artistId = artists.save(new Artist(tenantId, "Payment artist")).getId();
          Instant startsAt = Instant.parse("2031-02-01T10:00:00Z");
          Appointment appointment =
              appointments.save(
                  new Appointment(
                      tenantId,
                      customerId,
                      serviceId,
                      artistId,
                      startsAt,
                      startsAt.plusSeconds(3600)));
          return appointmentHolds.save(
              new AppointmentHold(
                  UUID.randomUUID(),
                  appointment.getId(),
                  startsAt.plusSeconds(1800),
                  "hold-" + UUID.randomUUID()));
        });
  }
}
