package com.emme.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.TestApplication;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.kernel.type.NotificationChannel;
import com.emme.notification.application.port.out.NotificationRepository;
import com.emme.notification.domain.model.Notification;
import com.emme.tenancy.adapter.out.client.database.TenantSchemaName;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
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
@DisplayName("notification integration test")
class NotificationIntegrationTest {

  @Autowired private DataSource dataSource;
  @Autowired private NotificationRepository notifications;
  @Autowired private TenantProvisioningRepository provisioningRepository;
  @Autowired private TenantSchemaMigrationPort schemaMigrationPort;

  @Autowired private PlatformTransactionManager transactionManager;

  @Autowired
  @Qualifier("bootstrapJdbcClient")
  private JdbcClient bootstrapJdbc;

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
  @DisplayName("JPA notification persistence uses the tenant selected schema")
  void notificationCrudUsesTheTenantSelectedSchema() {
    UUID tenantA = provisionTenant("notification-a");
    UUID tenantB = provisionTenant("notification-b");
    Notification saved =
        TenantContextHolder.withTenantOverride(
            tenantA,
            () ->
                notifications.save(
                    new Notification(tenantA, NotificationChannel.EMAIL, "recipient", "body")));

    TenantContextHolder.withTenantOverride(
        tenantB, () -> assertThat(notifications.findById(saved.id())).isEmpty());
    TenantContextHolder.withTenantOverride(
        tenantA,
        () ->
            assertThat(notifications.findById(saved.id()))
                .get()
                .extracting(Notification::tenantId)
                .isEqualTo(tenantA));
  }

  @Test
  @DisplayName("JPA notification updates reject a stale version")
  void concurrentNotificationUpdatesHaveOneOptimisticLockWinner() throws Exception {
    UUID tenantId = provisionTenant("notification-lock");
    UUID notificationId =
        TenantContextHolder.withTenantOverride(
            tenantId,
            () ->
                new TransactionTemplate(transactionManager)
                    .execute(
                        status ->
                            notifications
                                .save(
                                    new Notification(
                                        tenantId, NotificationChannel.EMAIL, "recipient", "body"))
                                .id()));
    CountDownLatch bothLoaded = new CountDownLatch(2);
    CountDownLatch releaseWrites = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<Throwable> first =
          executor.submit(
              () -> updateNotification(tenantId, notificationId, bothLoaded, releaseWrites));
      Future<Throwable> second =
          executor.submit(
              () -> updateNotification(tenantId, notificationId, bothLoaded, releaseWrites));

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

  private Throwable updateNotification(
      UUID tenantId, UUID notificationId, CountDownLatch bothLoaded, CountDownLatch releaseWrites) {
    try {
      TenantContextHolder.withTenantOverride(
          tenantId,
          () ->
              new TransactionTemplate(transactionManager)
                  .executeWithoutResult(
                      status -> {
                        Notification notification =
                            notifications.findById(notificationId).orElseThrow();
                        bothLoaded.countDown();
                        await(releaseWrites);
                        notification.markSent();
                        notifications.save(notification);
                      }));
      return null;
    } catch (Throwable failure) {
      return failure;
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timed out waiting for concurrent notification updates");
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(
          "Interrupted while coordinating concurrent notification updates", interrupted);
    }
  }

  private UUID provisionTenant(String prefix) {
    UUID tenantId = UUID.randomUUID();
    String slug = prefix + "-" + tenantId.toString().replace('-', 'a');
    String schemaName = TenantSchemaName.fromSlug(slug);
    bootstrapJdbc.sql("CREATE EXTENSION IF NOT EXISTS vector").update();
    provisioningRepository.requestProvisioning(tenantId, slug, schemaName);
    assertThat(schemaMigrationPort.migrate(tenantId, slug)).isEqualTo(schemaName);
    return tenantId;
  }
}
