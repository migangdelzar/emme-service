package com.emme.studio.application.service;

import com.emme.studio.api.result.AppointmentDetails;
import com.emme.studio.api.usecase.GetAppointmentUseCase;
import com.emme.studio.application.port.out.AppointmentCollisionPort;
import com.emme.studio.application.port.out.AppointmentRepository;
import com.emme.studio.application.port.out.ArtistRepository;
import com.emme.studio.application.port.out.CustomerRepository;
import com.emme.studio.application.port.out.ServiceRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for appointment retrieval. */
@Service
@Transactional(readOnly = true)
public class GetAppointmentService implements GetAppointmentUseCase {

  private final AppointmentRepository repository;
  private final AppointmentApplicationSupport support;

  public GetAppointmentService(
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
  public Optional<AppointmentDetails> get(UUID id) {
    return repository.findById(id).map(support::toDetails);
  }
}
