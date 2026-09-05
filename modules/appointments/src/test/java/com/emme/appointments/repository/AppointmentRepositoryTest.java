package com.emme.appointments.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.appointments.adapter.out.persistence.entity.AppointmentEntity;
import com.emme.appointments.adapter.out.persistence.repository.SpringDataAppointmentRepository;
import com.emme.appointments.domain.model.AppointmentStatus;
import com.emme.clients.adapter.out.persistence.entity.CustomerEntity;
import com.emme.clients.adapter.out.persistence.repository.SpringDataCustomerRepository;
import com.emme.services.adapter.out.persistence.entity.ArtistEntity;
import com.emme.services.adapter.out.persistence.entity.ServiceEntity;
import com.emme.services.adapter.out.persistence.repository.SpringDataArtistRepository;
import com.emme.services.adapter.out.persistence.repository.SpringDataServiceRepository;
import com.emme.testing.BaseRepositoryTest;
import com.emme.testing.TestSecurityConfig;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(TestSecurityConfig.class)
class AppointmentRepositoryTest extends BaseRepositoryTest {

  @Autowired private SpringDataAppointmentRepository appointmentRepo;

  @Autowired private SpringDataArtistRepository artistRepo;

  @Autowired private SpringDataCustomerRepository customerRepo;

  @Autowired private SpringDataServiceRepository serviceRepo;

  private UUID tenantId;
  private ArtistEntity artist;
  private CustomerEntity customer;
  private ServiceEntity service;

  @BeforeEach
  void setUp() {
    tenantId = UUID.randomUUID();
    artist = artistRepo.save(new ArtistEntity(tenantId, "Repo Artist"));
    customer = customerRepo.save(new CustomerEntity(tenantId, "Repo Customer"));
    service =
        serviceRepo.save(
            new ServiceEntity(tenantId, "r-cut", "Repo Cut", 30, new BigDecimal("25.00")));
  }

  @Test
  void shouldSaveAndFindAppointment() {
    Instant startsAt = Instant.now().plus(1, ChronoUnit.DAYS);
    Instant endsAt = startsAt.plus(1, ChronoUnit.HOURS);

    AppointmentEntity appointment =
        new AppointmentEntity(
            tenantId, customer.getId(), service.getId(), artist.getId(), startsAt, endsAt);
    AppointmentEntity saved = appointmentRepo.save(appointment);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);

    AppointmentEntity found = appointmentRepo.findById(saved.getId()).orElseThrow();
    assertThat(found.getCustomerId()).isEqualTo(customer.getId());
    assertThat(found.getArtistId()).isEqualTo(artist.getId());
  }

  @Test
  void shouldFindAppointmentsByArtistAndDateRange() {
    // PostgreSQL TIMESTAMPTZ and H2 timestamp values are persisted with microsecond precision.
    // Use the same precision for the inclusive lower-bound parameter so the test does not depend
    // on nanoseconds that the database cannot retain.
    Instant now = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
    Instant start1 = now;
    Instant end1 = now.plus(1, ChronoUnit.HOURS);
    Instant start2 = now.plus(3, ChronoUnit.HOURS);
    Instant end2 = now.plus(4, ChronoUnit.HOURS);

    appointmentRepo.save(
        new AppointmentEntity(
            tenantId, customer.getId(), service.getId(), artist.getId(), start1, end1));
    appointmentRepo.save(
        new AppointmentEntity(
            tenantId, customer.getId(), service.getId(), artist.getId(), start2, end2));

    List<AppointmentEntity> results =
        appointmentRepo.findByStartsAtBetween(now, now.plus(5, ChronoUnit.HOURS));

    assertThat(results).hasSize(2);
  }

  @Test
  void shouldFindActiveCollisionAtOverlapBoundary() {
    Instant now = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
    appointmentRepo.save(
        new AppointmentEntity(
            tenantId,
            customer.getId(),
            service.getId(),
            artist.getId(),
            now,
            now.plus(2, ChronoUnit.HOURS)));

    assertThat(
            appointmentRepo
                .existsByTenantIdAndArtistIdAndStartsAtLessThanAndEndsAtGreaterThanAndStatusIn(
                    tenantId,
                    artist.getId(),
                    now.plus(3, ChronoUnit.HOURS),
                    now.plus(1, ChronoUnit.HOURS),
                    List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS)))
        .isTrue();
    assertThat(
            appointmentRepo
                .existsByTenantIdAndArtistIdAndStartsAtLessThanAndEndsAtGreaterThanAndStatusIn(
                    tenantId,
                    artist.getId(),
                    now,
                    now.minus(1, ChronoUnit.HOURS),
                    List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS)))
        .isFalse();
  }

  @Test
  void shouldFindOnlyActiveCollisionsWithoutLoadingEntities() {
    UUID artistId = artist.getId();
    Instant start = Instant.parse("2030-01-01T10:00:00Z");
    Instant end = Instant.parse("2030-01-01T11:00:00Z");
    appointmentRepo.save(
        appointment(
            start.minusSeconds(900), end.minusSeconds(900), artistId, AppointmentStatus.CANCELLED));
    AppointmentEntity active =
        appointmentRepo.save(
            appointment(
                start.plusSeconds(900),
                end.plusSeconds(900),
                artistId,
                AppointmentStatus.CONFIRMED));

    assertThat(
            appointmentRepo
                .existsByTenantIdAndArtistIdAndStartsAtLessThanAndEndsAtGreaterThanAndStatusIn(
                    tenantId,
                    artistId,
                    end,
                    start,
                    List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS)))
        .isTrue();
    assertThat(
            appointmentRepo
                .existsByTenantIdAndArtistIdAndStartsAtLessThanAndEndsAtGreaterThanAndStatusInAndIdNot(
                    tenantId,
                    artistId,
                    end,
                    start,
                    List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS),
                    active.getId()))
        .isFalse();
  }

  @Test
  void cancelledAppointmentDoesNotCauseCollision() {
    UUID artistId = artist.getId();
    Instant start = Instant.parse("2030-01-01T10:00:00Z");
    Instant end = Instant.parse("2030-01-01T11:00:00Z");
    appointmentRepo.save(appointment(start, end, artistId, AppointmentStatus.CANCELLED));

    assertThat(
            appointmentRepo
                .existsByTenantIdAndArtistIdAndStartsAtLessThanAndEndsAtGreaterThanAndStatusIn(
                    tenantId,
                    artistId,
                    end,
                    start,
                    List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS)))
        .isFalse();
  }

  @Test
  void appointmentForAnotherTenantDoesNotCauseCollision() {
    UUID otherTenantId = UUID.randomUUID();
    UUID artistId = artist.getId();
    Instant start = Instant.parse("2030-01-01T10:00:00Z");
    Instant end = Instant.parse("2030-01-01T11:00:00Z");
    appointmentRepo.save(
        appointment(otherTenantId, start, end, artistId, AppointmentStatus.CONFIRMED));

    assertThat(
            appointmentRepo
                .existsByTenantIdAndArtistIdAndStartsAtLessThanAndEndsAtGreaterThanAndStatusIn(
                    tenantId,
                    artistId,
                    end,
                    start,
                    List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS)))
        .isFalse();
  }

  @Test
  void appointmentForAnotherArtistDoesNotCauseCollision() {
    ArtistEntity otherArtist = artistRepo.save(new ArtistEntity(tenantId, "Other Artist"));
    Instant start = Instant.parse("2030-01-01T10:00:00Z");
    Instant end = Instant.parse("2030-01-01T11:00:00Z");
    appointmentRepo.save(
        appointment(tenantId, start, end, otherArtist.getId(), AppointmentStatus.CONFIRMED));

    assertThat(
            appointmentRepo
                .existsByTenantIdAndArtistIdAndStartsAtLessThanAndEndsAtGreaterThanAndStatusIn(
                    tenantId,
                    artist.getId(),
                    end,
                    start,
                    List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS)))
        .isFalse();
  }

  @Test
  void adjacentAppointmentsDoNotCollide() {
    UUID artistId = artist.getId();
    Instant start = Instant.parse("2030-01-01T10:00:00Z");
    Instant end = Instant.parse("2030-01-01T11:00:00Z");
    appointmentRepo.save(appointment(start, end, artistId, AppointmentStatus.CONFIRMED));

    assertThat(
            appointmentRepo
                .existsByTenantIdAndArtistIdAndStartsAtLessThanAndEndsAtGreaterThanAndStatusIn(
                    tenantId,
                    artistId,
                    end.plusSeconds(3600),
                    end,
                    List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS)))
        .isFalse();
  }

  private AppointmentEntity appointment(
      Instant startsAt, Instant endsAt, UUID artistId, AppointmentStatus status) {
    return appointment(tenantId, startsAt, endsAt, artistId, status);
  }

  private AppointmentEntity appointment(
      UUID appointmentTenantId,
      Instant startsAt,
      Instant endsAt,
      UUID artistId,
      AppointmentStatus status) {
    AppointmentEntity appointment =
        new AppointmentEntity(
            appointmentTenantId, customer.getId(), service.getId(), artistId, startsAt, endsAt);
    appointment.setStatus(status);
    return appointment;
  }
}
