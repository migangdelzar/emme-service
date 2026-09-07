package com.emme.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.TestApplication;
import com.emme.calendar.api.type.GoogleOAuthPersona;
import com.emme.calendar.application.port.out.CalendarSyncStateRepository;
import com.emme.calendar.application.port.out.GoogleOAuthPort;
import com.emme.calendar.application.port.out.GoogleOAuthTokens;
import com.emme.calendar.domain.model.CalendarProvider;
import com.emme.calendar.domain.model.CalendarSyncState;
import com.emme.kernel.context.TenantContextHolder;
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
@DisplayName("calendar integration test")
class CalendarIntegrationTest {

  @Autowired private DataSource dataSource;
  @Autowired private CalendarSyncStateRepository syncStates;
  @Autowired private GoogleOAuthPort googleOAuth;
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
  @DisplayName("JPA calendar sync persistence uses the tenant selected schema")
  void calendarSyncStateUsesTheTenantSelectedSchema() {
    UUID tenantA = provisionTenant("calendar-a");
    UUID tenantB = provisionTenant("calendar-b");
    CalendarSyncState saved =
        TenantContextHolder.withTenantOverride(
            tenantA,
            () ->
                syncStates.save(
                    CalendarSyncState.active(tenantA, CalendarProvider.GOOGLE_CALENDAR)));

    TenantContextHolder.withTenantOverride(
        tenantB,
        () -> assertThat(syncStates.findByProvider(CalendarProvider.GOOGLE_CALENDAR)).isEmpty());
    TenantContextHolder.withTenantOverride(
        tenantA,
        () ->
            assertThat(syncStates.findByProvider(CalendarProvider.GOOGLE_CALENDAR))
                .get()
                .extracting(CalendarSyncState::id)
                .isEqualTo(saved.id()));
  }

  @Test
  @DisplayName("Google OAuth credentials are isolated by tenant schema")
  void googleOAuthCredentialsUseTheTenantSelectedSchema() {
    UUID tenantA = provisionTenant("calendar-oauth-a");
    UUID tenantB = provisionTenant("calendar-oauth-b");
    String userId = "calendar-oauth-user";
    GoogleOAuthTokens tokens =
        new GoogleOAuthTokens("access-token", "refresh-token", "calendar", 3600, "user@test");

    TenantContextHolder.withTenantOverride(
        tenantA, () -> googleOAuth.storeToken(tenantA, userId, GoogleOAuthPersona.CLIENT, tokens));
    TenantContextHolder.withTenantOverride(
        tenantB, () -> googleOAuth.storeToken(tenantB, userId, GoogleOAuthPersona.CLIENT, tokens));

    TenantContextHolder.withTenantOverride(
        tenantA,
        () -> {
          assertThat(googleOAuth.isConnected(tenantA, userId, GoogleOAuthPersona.CLIENT)).isTrue();
          assertThat(googleOAuth.isConnected(tenantB, userId, GoogleOAuthPersona.CLIENT)).isFalse();
        });
    TenantContextHolder.withTenantOverride(
        tenantB,
        () ->
            assertThat(googleOAuth.isConnected(tenantB, userId, GoogleOAuthPersona.CLIENT))
                .isTrue());
  }

  @Test
  @DisplayName("JPA calendar sync updates reject a stale version")
  void concurrentCalendarSyncUpdatesHaveOneOptimisticLockWinner() throws Exception {
    UUID tenantId = provisionTenant("calendar-lock");
    UUID stateId =
        TenantContextHolder.withTenantOverride(
            tenantId,
            () ->
                new TransactionTemplate(transactionManager)
                    .execute(
                        status ->
                            syncStates
                                .save(
                                    CalendarSyncState.active(
                                        tenantId, CalendarProvider.GOOGLE_CALENDAR))
                                .id()));
    CountDownLatch bothLoaded = new CountDownLatch(2);
    CountDownLatch releaseWrites = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<Throwable> first =
          executor.submit(() -> updateCalendarSync(tenantId, stateId, bothLoaded, releaseWrites));
      Future<Throwable> second =
          executor.submit(() -> updateCalendarSync(tenantId, stateId, bothLoaded, releaseWrites));

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

  private Throwable updateCalendarSync(
      UUID tenantId, UUID stateId, CountDownLatch bothLoaded, CountDownLatch releaseWrites) {
    try {
      TenantContextHolder.withTenantOverride(
          tenantId,
          () ->
              new TransactionTemplate(transactionManager)
                  .executeWithoutResult(
                      status -> {
                        CalendarSyncState state =
                            syncStates
                                .findByProvider(CalendarProvider.GOOGLE_CALENDAR)
                                .orElseThrow();
                        assertThat(state.id()).isEqualTo(stateId);
                        bothLoaded.countDown();
                        await(releaseWrites);
                        state.updateSync(state.syncToken(), java.time.Instant.now());
                        syncStates.save(state);
                      }));
      return null;
    } catch (Throwable failure) {
      return failure;
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timed out waiting for concurrent calendar updates");
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(
          "Interrupted while coordinating concurrent calendar updates", interrupted);
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
