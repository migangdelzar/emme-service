package com.emme.appointments.application.service;

import com.emme.appointments.api.result.AppointmentSummary;
import com.emme.appointments.api.usecase.ListAppointmentsUseCase;
import com.emme.appointments.application.port.out.AppointmentRepository;
import com.emme.appointments.domain.model.Appointment;
import com.emme.clients.application.port.out.CustomerRepository;
import com.emme.clients.domain.model.Customer;
import com.emme.services.application.port.out.ArtistRepository;
import com.emme.services.application.port.out.ServiceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for the public appointment-list query. */
@Service
@Transactional(readOnly = true)
public class ListAppointmentsService implements ListAppointmentsUseCase {

  private final AppointmentRepository appointmentRepository;
  private final CustomerRepository customerRepository;
  private final ServiceRepository serviceRepository;
  private final ArtistRepository artistRepository;

  public ListAppointmentsService(
      AppointmentRepository appointmentRepository,
      CustomerRepository customerRepository,
      ServiceRepository serviceRepository,
      ArtistRepository artistRepository) {
    this.appointmentRepository = appointmentRepository;
    this.customerRepository = customerRepository;
    this.serviceRepository = serviceRepository;
    this.artistRepository = artistRepository;
  }

  @Override
  public List<AppointmentSummary> listAppointments(UUID tenantId) {
    return appointmentRepository.findByTenantIdOrderByStartsAtDesc(tenantId).stream()
        .map(this::toAppointmentSummary)
        .toList();
  }

  private AppointmentSummary toAppointmentSummary(Appointment appointment) {
    String customerName =
        customerRepository
            .findById(appointment.getCustomerId())
            .map(Customer::getName)
            .orElse(null);
    String serviceName =
        serviceRepository
            .findById(appointment.getServiceId())
            .map(com.emme.services.domain.model.Service::getName)
            .orElse(null);
    String artistName =
        artistRepository
            .findById(appointment.getArtistId())
            .map(com.emme.services.domain.model.Artist::getName)
            .orElse(null);
    return new AppointmentSummary(
        appointment.getId(),
        appointment.getStartsAt(),
        appointment.getEndsAt(),
        customerName,
        serviceName,
        artistName,
        appointment.getStatus().name());
  }
}
