package com.emme.clients;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.TestApplication;
import com.emme.clients.adapter.out.persistence.entity.CustomerEntity;
import com.emme.clients.adapter.out.persistence.repository.SpringDataCustomerRepository;
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
class CustomerPersistenceIntegrationTest {

  @Autowired private SpringDataCustomerRepository customerRepository;
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
  void customerCrudUsesTheTenantSelectedSchema() {
    UUID tenantA = provisionTenant("customer-a");
    UUID tenantB = provisionTenant("customer-b");

    UUID customerId =
        TenantContextHolder.withTenantOverride(
            tenantA,
            () -> customerRepository.save(new CustomerEntity(tenantA, "Tenant A")).getId());

    TenantContextHolder.withTenantOverride(
        tenantB, () -> assertThat(customerRepository.findById(customerId)).isEmpty());

    TenantContextHolder.withTenantOverride(
        tenantA,
        () ->
            assertThat(customerRepository.findById(customerId))
                .get()
                .extracting(CustomerEntity::getName)
                .isEqualTo("Tenant A"));
  }

  @Test
  void concurrentCustomerUpdatesHaveOneOptimisticLockWinner() throws Exception {
    UUID tenantId = provisionTenant("customer-lock");
    UUID customerId =
        TenantContextHolder.withTenantOverride(
            tenantId,
            () ->
                new TransactionTemplate(transactionManager)
                    .execute(
                        status ->
                            customerRepository
                                .save(new CustomerEntity(tenantId, "Initial"))
                                .getId()));
    CountDownLatch bothLoaded = new CountDownLatch(2);
    CountDownLatch releaseWrites = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<Throwable> first =
          executor.submit(
              () -> updateCustomer(tenantId, customerId, "First", bothLoaded, releaseWrites));
      Future<Throwable> second =
          executor.submit(
              () -> updateCustomer(tenantId, customerId, "Second", bothLoaded, releaseWrites));

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

  private Throwable updateCustomer(
      UUID tenantId,
      UUID customerId,
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
                        CustomerEntity customer =
                            customerRepository.findById(customerId).orElseThrow();
                        bothLoaded.countDown();
                        await(releaseWrites);
                        customer.setName(name);
                        customerRepository.flush();
                      }));
      return null;
    } catch (Throwable failure) {
      return failure;
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timed out waiting for concurrent customer updates");
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(
          "Interrupted while coordinating customer updates", interrupted);
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
