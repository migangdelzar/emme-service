package com.emme.studio.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.studio.entity.Appointment;
import com.emme.studio.entity.AppointmentRepository;
import com.emme.studio.entity.AppointmentStatus;
import com.emme.studio.entity.Artist;
import com.emme.studio.entity.ArtistRepository;
import com.emme.studio.entity.Customer;
import com.emme.studio.entity.CustomerRepository;
import com.emme.studio.entity.Service;
import com.emme.studio.entity.ServiceRepository;
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

  @Autowired private AppointmentRepository appointmentRepo;

  @Autowired private ArtistRepository artistRepo;

  @Autowired private CustomerRepository customerRepo;

  @Autowired private ServiceRepository serviceRepo;

  private UUID tenantId;
  private Artist artist;
  private Customer customer;
  private Service service;

  @BeforeEach
  void setUp() {
    tenantId = UUID.randomUUID();
    artist = artistRepo.save(new Artist(tenantId, "Repo Artist"));
    customer = customerRepo.save(new Customer(tenantId, "Repo Customer"));
    service =
        serviceRepo.save(new Service(tenantId, "r-cut", "Repo Cut", 30, new BigDecimal("25.00")));
  }

  @Test
  void shouldSaveAndFindAppointment() {
    Instant startsAt = Instant.now().plus(1, ChronoUnit.DAYS);
    Instant endsAt = startsAt.plus(1, ChronoUnit.HOURS);

    Appointment appointment =
        new Appointment(tenantId, customer, service, artist, startsAt, endsAt);
    Appointment saved = appointmentRepo.save(appointment);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);

    Appointment found = appointmentRepo.findById(saved.getId()).orElseThrow();
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

    appointmentRepo.save(new Appointment(tenantId, customer, service, artist, start1, end1));
    appointmentRepo.save(new Appointment(tenantId, customer, service, artist, start2, end2));

    List<Appointment> results =
        appointmentRepo.findByArtistIdAndStartsAtBetween(
            artist.getId(), now, now.plus(5, ChronoUnit.HOURS));

    assertThat(results).hasSize(2);
  }
}
