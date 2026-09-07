package com.emme.subscriptions;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.TestApplication;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.subscriptions.api.type.PlanType;
import com.emme.subscriptions.application.port.out.SubscriptionRepository;
import com.emme.subscriptions.domain.model.Subscription;
import com.emme.tenancy.adapter.out.client.database.TenantSchemaName;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
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
@DisplayName("subscription integration test")
class SubscriptionIntegrationTest {

  @Autowired private DataSource dataSource;
  @Autowired private SubscriptionRepository subscriptions;
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
  @DisplayName("JPA subscription persistence uses the tenant selected schema")
  void subscriptionSingletonUsesTheTenantSelectedSchema() {
    UUID tenantA = provisionTenant("subscription-a");
    UUID tenantB = provisionTenant("subscription-b");
    Subscription saved =
        TenantContextHolder.withTenantOverride(
            tenantA,
            () ->
                subscriptions.save(
                    Subscription.provisioned(
                        tenantA,
                        PlanType.STARTER,
                        Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS))));

    TenantContextHolder.withTenantOverride(
        tenantB, () -> assertThat(subscriptions.find()).isEmpty());
    TenantContextHolder.withTenantOverride(
        tenantA,
        () ->
            assertThat(subscriptions.find())
                .get()
                .extracting(Subscription::id)
                .isEqualTo(saved.id()));
  }

  @Test
  @DisplayName("JPA subscription updates reject a stale version")
  void concurrentSubscriptionUpdatesHaveOneOptimisticLockWinner() throws Exception {
    UUID tenantId = provisionTenant("subscription-lock");
    UUID subscriptionId =
        TenantContextHolder.withTenantOverride(
            tenantId,
            () ->
                new TransactionTemplate(transactionManager)
                    .execute(
                        status ->
                            subscriptions
                                .save(
                                    Subscription.provisioned(
                                        tenantId,
                                        PlanType.STARTER,
                                        Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS)))
                                .id()));
    CountDownLatch bothLoaded = new CountDownLatch(2);
    CountDownLatch releaseWrites = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<Throwable> first =
          executor.submit(
              () -> updateSubscription(tenantId, subscriptionId, bothLoaded, releaseWrites));
      Future<Throwable> second =
          executor.submit(
              () -> updateSubscription(tenantId, subscriptionId, bothLoaded, releaseWrites));

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

  private Throwable updateSubscription(
      UUID tenantId, UUID subscriptionId, CountDownLatch bothLoaded, CountDownLatch releaseWrites) {
    try {
      TenantContextHolder.withTenantOverride(
          tenantId,
          () ->
              new TransactionTemplate(transactionManager)
                  .executeWithoutResult(
                      status -> {
                        Subscription subscription = subscriptions.find().orElseThrow();
                        assertThat(subscription.id()).isEqualTo(subscriptionId);
                        bothLoaded.countDown();
                        await(releaseWrites);
                        subscription.changePlan(PlanType.PRO);
                        subscriptions.save(subscription);
                      }));
      return null;
    } catch (Throwable failure) {
      return failure;
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timed out waiting for concurrent subscription updates");
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(
          "Interrupted while coordinating concurrent subscription updates", interrupted);
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
