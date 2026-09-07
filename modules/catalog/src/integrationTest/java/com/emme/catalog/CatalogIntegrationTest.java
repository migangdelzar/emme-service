package com.emme.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.TestApplication;
import com.emme.catalog.application.port.out.CatalogItemRepository;
import com.emme.catalog.domain.model.CatalogItem;
import com.emme.catalog.domain.model.CatalogItemStatus;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.services.application.port.out.ServiceRepository;
import com.emme.services.domain.model.Service;
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
@DisplayName("catalog integration test")
class CatalogIntegrationTest {

  @Autowired private DataSource dataSource;
  @Autowired private CatalogItemRepository catalogItems;
  @Autowired private ServiceRepository services;
  @Autowired private TenantProvisioningRepository provisioningRepository;
  @Autowired private TenantSchemaMigrationPort schemaMigrationPort;
  @Autowired private PlatformTransactionManager transactionManager;

  @Autowired
  @Qualifier("bootstrapJdbcClient")
  private JdbcClient bootstrapJdbc;

  @BeforeEach
  void installDatabaseExtensions() {
    bootstrapJdbc.sql("CREATE EXTENSION IF NOT EXISTS vector SCHEMA emme_core").update();
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
  @DisplayName("JPA catalog persistence uses the tenant selected schema")
  void catalogItemCrudUsesTheTenantSelectedSchema() {
    UUID tenantA = provisionTenant("catalog-a");
    UUID tenantB = provisionTenant("catalog-b");
    UUID serviceId =
        TenantContextHolder.withTenantOverride(
            tenantA,
            () ->
                services
                    .save(
                        new Service(
                            tenantA,
                            "CATALOG-SERVICE",
                            "Catalog service",
                            60,
                            new BigDecimal("100.00")))
                    .getId());

    CatalogItem saved =
        TenantContextHolder.withTenantOverride(
            tenantA,
            () ->
                catalogItems.save(
                    new CatalogItem(
                        tenantA,
                        serviceId,
                        "CATALOG-ITEM",
                        "Catalog item",
                        "Description",
                        new BigDecimal("100.00"),
                        null,
                        60,
                        null)));
    UUID catalogItemId = saved.getId();

    TenantContextHolder.withTenantOverride(
        tenantB, () -> assertThat(catalogItems.findById(catalogItemId)).isEmpty());
    TenantContextHolder.withTenantOverride(
        tenantA,
        () ->
            assertThat(catalogItems.findById(catalogItemId))
                .get()
                .extracting(CatalogItem::getTenantId)
                .isEqualTo(tenantA));
  }

  @Test
  @DisplayName("JPA catalog updates reject a stale version")
  void concurrentCatalogUpdatesHaveOneOptimisticLockWinner() throws Exception {
    UUID tenantId = provisionTenant("catalog-lock");
    UUID serviceId =
        TenantContextHolder.withTenantOverride(
            tenantId,
            () ->
                services
                    .save(
                        new Service(
                            tenantId,
                            "CATALOG-LOCK-SERVICE",
                            "Catalog lock service",
                            60,
                            new BigDecimal("100.00")))
                    .getId());
    UUID catalogItemId =
        TenantContextHolder.withTenantOverride(
            tenantId,
            () ->
                new TransactionTemplate(transactionManager)
                    .execute(
                        status ->
                            catalogItems
                                .save(
                                    new CatalogItem(
                                        tenantId,
                                        serviceId,
                                        "CATALOG-LOCK-ITEM",
                                        "Catalog lock item",
                                        "Description",
                                        new BigDecimal("100.00"),
                                        null,
                                        60,
                                        null))
                                .getId()));
    CountDownLatch bothLoaded = new CountDownLatch(2);
    CountDownLatch releaseWrites = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<Throwable> first =
          executor.submit(
              () -> updateCatalogItem(tenantId, catalogItemId, bothLoaded, releaseWrites));
      Future<Throwable> second =
          executor.submit(
              () -> updateCatalogItem(tenantId, catalogItemId, bothLoaded, releaseWrites));

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

  private Throwable updateCatalogItem(
      UUID tenantId, UUID catalogItemId, CountDownLatch bothLoaded, CountDownLatch releaseWrites) {
    try {
      TenantContextHolder.withTenantOverride(
          tenantId,
          () ->
              new TransactionTemplate(transactionManager)
                  .executeWithoutResult(
                      status -> {
                        CatalogItem item = catalogItems.findById(catalogItemId).orElseThrow();
                        bothLoaded.countDown();
                        await(releaseWrites);
                        item.changeStatus(CatalogItemStatus.RETIRED);
                        catalogItems.save(item);
                      }));
      return null;
    } catch (Throwable failure) {
      return failure;
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timed out waiting for concurrent catalog updates");
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(
          "Interrupted while coordinating concurrent catalog updates", interrupted);
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
