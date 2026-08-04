package com.emme.studio.application.service;

import com.emme.studio.api.result.AppointmentDetails;
import com.emme.studio.api.usecase.MarkAppointmentNoShowUseCase;
import com.emme.studio.application.port.out.AppointmentCollisionPort;
import com.emme.studio.application.port.out.AppointmentRepository;
import com.emme.studio.application.port.out.ArtistRepository;
import com.emme.studio.application.port.out.CustomerRepository;
import com.emme.studio.application.port.out.ServiceRepository;
import com.emme.studio.domain.model.Appointment;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for marking appointments as no-show. */
@Service
@Transactional
public class MarkAppointmentNoShowService implements MarkAppointmentNoShowUseCase {

  private final AppointmentRepository repository;
  private final AppointmentApplicationSupport support;

  public MarkAppointmentNoShowService(
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
  public AppointmentDetails markNoShow(UUID id) {
    Appointment appointment = support.find(id);
    appointment.noShow();
    return support.toDetails(repository.save(appointment));
  }
}
