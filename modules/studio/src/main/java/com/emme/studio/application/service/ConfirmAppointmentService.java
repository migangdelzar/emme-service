package com.emme.studio.application.service;

import com.emme.studio.api.usecase.ConfirmAppointmentUseCase;
import com.emme.studio.application.port.out.AppointmentCollisionPort;
import com.emme.studio.application.port.out.AppointmentRepository;
import com.emme.studio.application.port.out.ArtistRepository;
import com.emme.studio.application.port.out.CustomerRepository;
import com.emme.studio.application.port.out.ServiceRepository;
import com.emme.studio.application.result.AppointmentView;
import com.emme.studio.domain.model.Appointment;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for appointment confirmation. */
@Service
@Transactional
public class ConfirmAppointmentService implements ConfirmAppointmentUseCase {

  private final AppointmentRepository repository;
  private final AppointmentApplicationSupport support;

  public ConfirmAppointmentService(
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
  public AppointmentView confirm(UUID id) {
    Appointment appointment = support.find(id);
    appointment.confirm();
    return support.toView(repository.save(appointment));
  }
}
