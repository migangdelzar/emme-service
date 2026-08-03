package com.emme.studio.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.studio.adapter.out.persistence.entity.AppointmentEntity;
import com.emme.studio.adapter.out.persistence.entity.ArtistEntity;
import com.emme.studio.adapter.out.persistence.entity.CustomerEntity;
import com.emme.studio.adapter.out.persistence.entity.ServiceEntity;
import com.emme.studio.adapter.out.persistence.repository.SpringDataAppointmentRepository;
import com.emme.studio.adapter.out.persistence.repository.SpringDataArtistRepository;
import com.emme.studio.adapter.out.persistence.repository.SpringDataCustomerRepository;
import com.emme.studio.adapter.out.persistence.repository.SpringDataServiceRepository;
import com.emme.studio.domain.model.AppointmentStatus;
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
        new AppointmentEntity(tenantId, customer, service, artist, startsAt, endsAt);
    AppointmentEntity saved = appointmentRepo.save(appointment);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);

    AppointmentEntity found = appointmentRepo.findById(saved.getId()).orElseThrow();
    assertThat(found.getCustomer().getName()).isEqualTo("Repo Customer");
    assertThat(found.getArtist().getName()).isEqualTo("Repo Artist");
  }

  @Test
  void shouldFindAppointmentsByArtistAndDateRange() {
    Instant now = Instant.now().plus(2, ChronoUnit.DAYS);
    Instant start1 = now;
    Instant end1 = now.plus(1, ChronoUnit.HOURS);
    Instant start2 = now.plus(3, ChronoUnit.HOURS);
    Instant end2 = now.plus(4, ChronoUnit.HOURS);

    appointmentRepo.save(new AppointmentEntity(tenantId, customer, service, artist, start1, end1));
    appointmentRepo.save(new AppointmentEntity(tenantId, customer, service, artist, start2, end2));

    List<AppointmentEntity> results =
        appointmentRepo.findByArtistIdAndStartsAtBetween(
            artist.getId(), now, now.plus(5, ChronoUnit.HOURS));

    assertThat(results).hasSize(2);
  }
}
