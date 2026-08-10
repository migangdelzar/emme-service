package com.emme.appointments.application.service;

import com.emme.appointments.api.result.AppointmentDetails;
import com.emme.appointments.application.mapper.AppointmentApplicationMapper;
import com.emme.appointments.application.port.out.AppointmentCollisionPort;
import com.emme.appointments.application.port.out.AppointmentRepository;
import com.emme.appointments.domain.model.Appointment;
import com.emme.clients.application.port.out.CustomerRepository;
import com.emme.clients.domain.model.Customer;
import com.emme.services.application.port.out.ArtistRepository;
import com.emme.services.application.port.out.ServiceRepository;
import com.emme.services.domain.model.Artist;
import java.time.Instant;
import java.util.UUID;

/** Shared application translation and validation for appointment use cases. */
final class AppointmentApplicationSupport {

  private final AppointmentRepository appointmentRepository;
  private final AppointmentCollisionPort collisionPort;
  private final CustomerRepository customerRepository;
  private final ServiceRepository serviceRepository;
  private final ArtistRepository artistRepository;

  AppointmentApplicationSupport(
      AppointmentRepository appointmentRepository,
      AppointmentCollisionPort collisionPort,
      CustomerRepository customerRepository,
      ServiceRepository serviceRepository,
      ArtistRepository artistRepository) {
    this.appointmentRepository = appointmentRepository;
    this.collisionPort = collisionPort;
    this.customerRepository = customerRepository;
    this.serviceRepository = serviceRepository;
    this.artistRepository = artistRepository;
  }

  Appointment find(UUID id) {
    return appointmentRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + id));
  }

  void ensureReferences(UUID customerId, UUID serviceId, UUID artistId) {
    if (customerRepository.findById(customerId).isEmpty()) {
      throw new IllegalArgumentException("Customer not found: " + customerId);
    }
    if (serviceRepository.findById(serviceId).isEmpty()) {
      throw new IllegalArgumentException("Service not found: " + serviceId);
    }
    if (artistRepository.findById(artistId).isEmpty()) {
      throw new IllegalArgumentException("Artist not found: " + artistId);
    }
  }

  void ensureAvailable(UUID artistId, Instant startsAt, Instant endsAt) {
    if (collisionPort.hasCollision(artistId, startsAt, endsAt)) {
      throw new IllegalStateException(
          "Slot conflict: artist "
              + artistId
              + " already has a confirmed appointment in this time range");
    }
  }

  AppointmentDetails toDetails(Appointment appointment) {
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
        artistRepository.findById(appointment.getArtistId()).map(Artist::getName).orElse(null);
    return AppointmentApplicationMapper.toDetails(
        appointment, customerName, serviceName, artistName);
  }
}
