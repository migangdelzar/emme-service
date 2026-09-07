package com.emme.appointments;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.TestApplication;
import com.emme.appointments.application.port.out.AppointmentRepository;
import com.emme.appointments.domain.model.Appointment;
import com.emme.clients.application.port.out.CustomerRepository;
import com.emme.clients.domain.model.Customer;
import com.emme.kernel.context.TenantContextHolder;
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
@DisplayName("appointments integration test")
class AppointmentIntegrationTest {

  @Autowired private DataSource dataSource;
  @Autowired private AppointmentRepository appointments;
  @Autowired private CustomerRepository customers;
  @Autowired private ServiceRepository services;
  @Autowired private ArtistRepository artists;
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
  @DisplayName("JPA appointment persistence uses the tenant selected schema")
  void appointmentCrudUsesTheTenantSelectedSchema() {
    UUID tenantA = provisionTenant("appointment-a");
    UUID tenantB = provisionTenant("appointment-b");
    Appointment saved =
        TenantContextHolder.withTenantOverride(tenantA, () -> createAppointment(tenantA));

    TenantContextHolder.withTenantOverride(
        tenantB, () -> assertThat(appointments.findById(saved.getId())).isEmpty());
    TenantContextHolder.withTenantOverride(
        tenantA,
        () ->
            assertThat(appointments.findById(saved.getId()))
                .get()
                .extracting(Appointment::getTenantId)
                .isEqualTo(tenantA));
  }

  @Test
  @DisplayName("JPA appointment updates reject a stale version")
  void concurrentAppointmentUpdatesHaveOneOptimisticLockWinner() throws Exception {
    UUID tenantId = provisionTenant("appointment-lock");
    Appointment saved =
        TenantContextHolder.withTenantOverride(tenantId, () -> createAppointment(tenantId));
    CountDownLatch bothLoaded = new CountDownLatch(2);
    CountDownLatch releaseWrites = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<Throwable> first =
          executor.submit(
              () -> updateAppointment(tenantId, saved.getId(), bothLoaded, releaseWrites));
      Future<Throwable> second =
          executor.submit(
              () -> updateAppointment(tenantId, saved.getId(), bothLoaded, releaseWrites));

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

  private Appointment createAppointment(UUID tenantId) {
    UUID customerId = customers.save(new Customer(tenantId, "Appointment customer")).getId();
    UUID serviceId =
        services
            .save(
                new Service(
                    tenantId, "APPOINTMENT-SERVICE", "Appointment service", 60, BigDecimal.TEN))
            .getId();
    UUID artistId = artists.save(new Artist(tenantId, "Appointment artist")).getId();
    Instant startsAt = Instant.parse("2031-01-01T10:00:00Z");
    return appointments.save(
        new Appointment(
            tenantId, customerId, serviceId, artistId, startsAt, startsAt.plusSeconds(3600)));
  }

  private Throwable updateAppointment(
      UUID tenantId, UUID appointmentId, CountDownLatch bothLoaded, CountDownLatch releaseWrites) {
    try {
      TenantContextHolder.withTenantOverride(
          tenantId,
          () ->
              new TransactionTemplate(transactionManager)
                  .executeWithoutResult(
                      status -> {
                        Appointment appointment =
                            appointments.findById(appointmentId).orElseThrow();
                        bothLoaded.countDown();
                        await(releaseWrites);
                        appointment.reschedule(
                            appointment.getStartsAt().plusSeconds(7200),
                            appointment.getEndsAt().plusSeconds(7200));
                        appointments.save(appointment);
                      }));
      return null;
    } catch (Throwable failure) {
      return failure;
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timed out waiting for concurrent appointment updates");
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(
          "Interrupted while coordinating concurrent appointment updates", interrupted);
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
