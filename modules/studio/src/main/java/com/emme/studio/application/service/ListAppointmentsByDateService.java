package com.emme.studio.application.service;

import com.emme.studio.api.result.AppointmentDetails;
import com.emme.studio.api.usecase.ListAppointmentsByDateUseCase;
import com.emme.studio.application.port.out.AppointmentCollisionPort;
import com.emme.studio.application.port.out.AppointmentRepository;
import com.emme.studio.application.port.out.ArtistRepository;
import com.emme.studio.application.port.out.CustomerRepository;
import com.emme.studio.application.port.out.ServiceRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for date-filtered appointment listing. */
@Service
@Transactional(readOnly = true)
public class ListAppointmentsByDateService implements ListAppointmentsByDateUseCase {

  private static final ZoneId STUDIO_ZONE = ZoneId.of("America/Mexico_City");
  private final AppointmentRepository repository;
  private final AppointmentApplicationSupport support;

  public ListAppointmentsByDateService(
      AppointmentRepository repository,
      AppointmentCollisionPort collisionPort,
      CustomerRepository customerRepository,
      ServiceRepository serviceRepository,
      ArtistRepository artistRepository) {
    this.repository = repository;
    this.support =
        new AppointmentApplicationSupport(
            repository, collisionPort, customerRepository, serviceRepository, artistRepository);
  }

  @Override
  public List<AppointmentDetails> list(UUID tenantId, LocalDate date) {
    var dayStart = ZonedDateTime.of(date.atStartOfDay(), STUDIO_ZONE).toInstant();
    var dayEnd = ZonedDateTime.of(date.plusDays(1).atStartOfDay(), STUDIO_ZONE).toInstant();
    return repository.findByTenantIdAndStartsAtBetween(tenantId, dayStart, dayEnd).stream()
        .map(support::toView)
        .toList();
  }
}
