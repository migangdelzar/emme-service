package com.emme.appointments.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class AppointmentCollisionConcurrencyIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("emme_test")
          .withUsername("emme")
          .withPassword("emme");

  private ExecutorService executor;

  @BeforeEach
  void createAppointmentTableAndConstraint() throws SQLException {
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE EXTENSION IF NOT EXISTS btree_gist");
      statement.execute("DROP TABLE IF EXISTS appointment");
      statement.execute(
          """
          CREATE TABLE appointment (
              id UUID PRIMARY KEY,
              tenant_id UUID NOT NULL,
              artist_id UUID NOT NULL,
              starts_at TIMESTAMPTZ NOT NULL,
              ends_at TIMESTAMPTZ NOT NULL,
              status VARCHAR(20) NOT NULL
          )
          """);
      statement.execute(
          """
          ALTER TABLE appointment
              ADD CONSTRAINT appointment_active_artist_no_overlap
              EXCLUDE USING gist (
                  tenant_id WITH =,
                  artist_id WITH =,
                  tstzrange(starts_at, ends_at, '[)') WITH &&
              )
              WHERE (status IN ('CONFIRMED','IN_PROGRESS'))
          """);
    }
    executor = Executors.newFixedThreadPool(2);
  }

  @AfterEach
  void shutDownExecutor() throws InterruptedException {
    executor.shutdownNow();
    assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void concurrentOverlappingActiveInsertsAllowOnlyOneCommit() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID artistId = UUID.randomUUID();
    Instant startsAt = Instant.parse("2030-01-01T10:00:00Z");
    Instant endsAt = Instant.parse("2030-01-01T11:00:00Z");
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    List<Future<InsertResult>> attempts =
        List.of(
            submitInsert(tenantId, artistId, startsAt, endsAt, ready, start),
            submitInsert(tenantId, artistId, startsAt, endsAt, ready, start));

    assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
    start.countDown();

    List<InsertResult> results = attempts.stream().map(this::result).toList();
    assertThat(results).extracting(InsertResult::committed).containsExactlyInAnyOrder(true, false);
    assertThat(results)
        .filteredOn(result -> !result.committed())
        .singleElement()
        .extracting(InsertResult::sqlState)
        .isEqualTo("23P01");
  }

  private Future<InsertResult> submitInsert(
      UUID tenantId,
      UUID artistId,
      Instant startsAt,
      Instant endsAt,
      CountDownLatch ready,
      CountDownLatch start) {
    return executor.submit(
        () -> {
          ready.countDown();
          start.await();
          return insert(tenantId, artistId, startsAt, endsAt);
        });
  }

  private InsertResult insert(UUID tenantId, UUID artistId, Instant startsAt, Instant endsAt) {
    try (Connection connection = connection();
        PreparedStatement statement =
            connection.prepareStatement(
                "INSERT INTO appointment "
                    + "(id, tenant_id, artist_id, starts_at, ends_at, status) "
                    + "VALUES (?, ?, ?, ?, ?, 'CONFIRMED')")) {
      connection.setAutoCommit(false);
      statement.setObject(1, UUID.randomUUID());
      statement.setObject(2, tenantId);
      statement.setObject(3, artistId);
      statement.setObject(4, startsAt);
      statement.setObject(5, endsAt);
      statement.executeUpdate();
      connection.commit();
      return new InsertResult(true, null);
    } catch (SQLException e) {
      return new InsertResult(false, e.getSQLState());
    }
  }

  private InsertResult result(Future<InsertResult> attempt) {
    try {
      return attempt.get(15, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new AssertionError("Concurrent insert attempt did not finish", e);
    }
  }

  private record InsertResult(boolean committed, String sqlState) {}

  private Connection connection() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }
}
