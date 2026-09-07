package com.emme.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.TestApplication;
import com.emme.identity.adapter.out.persistence.repository.SpringDataCustomerMembershipRepository;
import com.emme.identity.api.command.EnsureCustomerMembershipCommand;
import com.emme.identity.api.usecase.EnsureCustomerMembershipUseCase;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestApplication.class)
@PostgresIntegrationTest
@DisplayName("identity integration test")
class IdentityIntegrationTest {

  @Autowired private DataSource dataSource;

  @Autowired private EnsureCustomerMembershipUseCase ensureCustomerMembership;

  @Autowired private SpringDataCustomerMembershipRepository memberships;

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
  @DisplayName("duplicate appointment deliveries create one customer membership")
  void duplicateAppointmentDeliveriesAreSafeConcurrently() throws Exception {
    UUID customerId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    List<Future<?>> deliveries = new ArrayList<>();
    try {
      for (int i = 0; i < 2; i++) {
        deliveries.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  await(start);
                  ensureCustomerMembership.ensure(
                      new EnsureCustomerMembershipCommand(customerId, tenantId));
                }));
      }
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      for (Future<?> delivery : deliveries) {
        delivery.get(10, TimeUnit.SECONDS);
      }
    } finally {
      executor.shutdownNow();
    }

    assertThat(memberships.findByCustomerId(customerId)).hasSize(1);
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timed out waiting for duplicate delivery test");
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(
          "Interrupted while waiting for duplicate delivery", interrupted);
    }
  }
}
