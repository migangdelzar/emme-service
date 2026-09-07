package com.emme.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.TestApplication;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.services.adapter.out.persistence.entity.ServiceEntity;
import com.emme.services.adapter.out.persistence.repository.SpringDataServiceRepository;
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
class ServicePersistenceIntegrationTest {

  @Autowired private SpringDataServiceRepository serviceRepository;
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
  void serviceCrudUsesTheTenantSelectedSchema() {
    UUID tenantA = provisionTenant("service-a");
    UUID tenantB = provisionTenant("service-b");

    UUID serviceId =
        TenantContextHolder.withTenantOverride(
            tenantA,
            () ->
                serviceRepository
                    .save(new ServiceEntity(tenantA, "CUT", "Cut", 30, BigDecimal.valueOf(25)))
                    .getId());

    TenantContextHolder.withTenantOverride(
        tenantB, () -> assertThat(serviceRepository.findById(serviceId)).isEmpty());

    TenantContextHolder.withTenantOverride(
        tenantA,
        () ->
            assertThat(serviceRepository.findById(serviceId))
                .get()
                .extracting(ServiceEntity::getName)
                .isEqualTo("Cut"));
  }

  @Test
  void concurrentServiceUpdatesHaveOneOptimisticLockWinner() throws Exception {
    UUID tenantId = provisionTenant("service-lock");
    UUID serviceId =
        TenantContextHolder.withTenantOverride(
            tenantId,
            () ->
                new TransactionTemplate(transactionManager)
                    .execute(
                        status ->
                            serviceRepository
                                .save(
                                    new ServiceEntity(
                                        tenantId, "CUT", "Initial", 30, BigDecimal.valueOf(25)))
                                .getId()));
    CountDownLatch bothLoaded = new CountDownLatch(2);
    CountDownLatch releaseWrites = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<Throwable> first =
          executor.submit(
              () -> updateService(tenantId, serviceId, "First", bothLoaded, releaseWrites));
      Future<Throwable> second =
          executor.submit(
              () -> updateService(tenantId, serviceId, "Second", bothLoaded, releaseWrites));

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

  private Throwable updateService(
      UUID tenantId,
      UUID serviceId,
      String name,
      CountDownLatch bothLoaded,
      CountDownLatch releaseWrites) {
    try {
      TenantContextHolder.withTenantOverride(
          tenantId,
          () ->
              new TransactionTemplate(transactionManager)
                  .executeWithoutResult(
                      status -> {
                        ServiceEntity service = serviceRepository.findById(serviceId).orElseThrow();
                        bothLoaded.countDown();
                        await(releaseWrites);
                        service.setName(name);
                        serviceRepository.flush();
                      }));
      return null;
    } catch (Throwable failure) {
      return failure;
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timed out waiting for concurrent service updates");
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(
          "Interrupted while coordinating service updates", interrupted);
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
