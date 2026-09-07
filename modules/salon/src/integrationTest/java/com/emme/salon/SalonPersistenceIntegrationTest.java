package com.emme.salon;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.TestApplication;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.salon.adapter.out.persistence.entity.BusinessProfileEntity;
import com.emme.salon.adapter.out.persistence.repository.SpringDataBusinessProfileRepository;
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
import org.junit.jupiter.api.BeforeEach;
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
class SalonPersistenceIntegrationTest {

  @Autowired private SpringDataBusinessProfileRepository profileRepository;
  @Autowired private TenantProvisioningRepository provisioningRepository;
  @Autowired private TenantSchemaMigrationPort schemaMigrationPort;

  @Autowired
  @Qualifier("bootstrapJdbcClient")
  private JdbcClient bootstrapJdbc;

  @Autowired private PlatformTransactionManager transactionManager;

  @BeforeEach
  void installDatabaseExtensions() {
    bootstrapJdbc.sql("CREATE EXTENSION IF NOT EXISTS vector").update();
  }

  @Test
  void businessProfileSingletonUsesTheTenantSelectedSchema() {
    UUID tenantA = provisionTenant("salon-a");
    UUID tenantB = provisionTenant("salon-b");

    TenantContextHolder.withTenantOverride(
        tenantA,
        () ->
            profileRepository.save(
                new BusinessProfileEntity(tenantA, "America/Mexico_City", "es-MX", "Salon A")));

    TenantContextHolder.withTenantOverride(
        tenantB, () -> assertThat(profileRepository.findFirstByOrderByCreatedAtAsc()).isEmpty());

    TenantContextHolder.withTenantOverride(
        tenantA,
        () ->
            assertThat(profileRepository.findFirstByOrderByCreatedAtAsc())
                .get()
                .extracting(BusinessProfileEntity::getDisplayName)
                .isEqualTo("Salon A"));
  }

  @Test
  void concurrentBusinessProfileUpdatesHaveOneOptimisticLockWinner() throws Exception {
    UUID tenantId = provisionTenant("salon-lock");
    UUID profileId =
        TenantContextHolder.withTenantOverride(
            tenantId,
            () ->
                new TransactionTemplate(transactionManager)
                    .execute(
                        status ->
                            profileRepository
                                .save(
                                    new BusinessProfileEntity(
                                        tenantId, "America/Mexico_City", "es-MX", "Initial"))
                                .getId()));
    CountDownLatch bothLoaded = new CountDownLatch(2);
    CountDownLatch releaseWrites = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<Throwable> first =
          executor.submit(
              () -> updateBusinessProfile(tenantId, profileId, "First", bothLoaded, releaseWrites));
      Future<Throwable> second =
          executor.submit(
              () ->
                  updateBusinessProfile(tenantId, profileId, "Second", bothLoaded, releaseWrites));

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

  private Throwable updateBusinessProfile(
      UUID tenantId,
      UUID profileId,
      String displayName,
      CountDownLatch bothLoaded,
      CountDownLatch releaseWrites) {
    try {
      TenantContextHolder.withTenantOverride(
          tenantId,
          () ->
              new TransactionTemplate(transactionManager)
                  .executeWithoutResult(
                      status -> {
                        BusinessProfileEntity profile =
                            profileRepository.findById(profileId).orElseThrow();
                        bothLoaded.countDown();
                        await(releaseWrites);
                        profile.setDisplayName(displayName);
                        profileRepository.flush();
                      }));
      return null;
    } catch (Throwable failure) {
      return failure;
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException(
            "Timed out waiting for concurrent business-profile updates");
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(
          "Interrupted while coordinating business-profile updates", interrupted);
    }
  }

  private UUID provisionTenant(String prefix) {
    UUID tenantId = UUID.randomUUID();
    String slug = prefix + "-" + tenantId.toString().replace('-', 'a');
    provisioningRepository.requestProvisioning(tenantId, slug, TenantSchemaName.fromSlug(slug));
    assertThat(schemaMigrationPort.migrate(tenantId, slug))
        .isEqualTo(TenantSchemaName.fromSlug(slug));
    return tenantId;
  }
}
