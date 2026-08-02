package com.emme.studio.application.service;

import com.emme.studio.api.result.AppointmentDetails;
import com.emme.studio.api.usecase.CompleteAppointmentUseCase;
import com.emme.studio.application.port.out.AppointmentCollisionPort;
import com.emme.studio.application.port.out.AppointmentRepository;
import com.emme.studio.application.port.out.ArtistRepository;
import com.emme.studio.application.port.out.CustomerRepository;
import com.emme.studio.application.port.out.ServiceRepository;
import com.emme.studio.domain.model.Appointment;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for completing appointments. */
@Service
@Transactional
public class CompleteAppointmentService implements CompleteAppointmentUseCase {

  private final AppointmentRepository repository;
  private final AppointmentApplicationSupport support;

  public CompleteAppointmentService(
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
  public AppointmentDetails complete(UUID id) {
    Appointment appointment = support.find(id);
    appointment.complete();
    return support.toView(repository.save(appointment));
  }
}
