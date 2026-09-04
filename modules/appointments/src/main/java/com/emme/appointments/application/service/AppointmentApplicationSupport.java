package com.emme.appointments.application.service;

import com.emme.appointments.api.result.AppointmentDetails;
import com.emme.appointments.api.type.AppointmentActor;
import com.emme.appointments.application.mapper.AppointmentApplicationMapper;
import com.emme.appointments.application.port.out.AppointmentCollisionPort;
import com.emme.appointments.application.port.out.AppointmentRepository;
import com.emme.appointments.domain.model.Appointment;
import com.emme.appointments.domain.model.AppointmentStatus;
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

  Appointment authorize(AppointmentActor actor, UUID id) {
    Appointment appointment = find(id);
    if (!actor.tenantId().equals(appointment.getTenantId()))
      throw new SecurityException("Appointment is outside actor tenant");
    if (!actor.isStaff()
        && (!actor.hasRole("client") || !actor.principalId().equals(appointment.getCustomerId())))
      throw new SecurityException("Actor is not authorized for appointment");
    return appointment;
  }

  void ensureActorCanBook(AppointmentActor actor, UUID customerId, boolean confirmed) {
    if (!confirmed) throw new SecurityException("User confirmation is required");
    if (!actor.isStaff() && (!actor.hasRole("client") || !actor.principalId().equals(customerId)))
      throw new SecurityException("Actor is not authorized to book for customer");
    if (customerRepository.findByTenantIdAndId(actor.tenantId(), customerId).isEmpty())
      throw new SecurityException("Customer is outside actor tenant");
  }

  void ensureMutable(Appointment appointment) {
    if (appointment.getStatus() != AppointmentStatus.CONFIRMED)
      throw new IllegalStateException("Only confirmed appointments can be mutated");
  }

  void ensureReferences(UUID tenantId, UUID customerId, UUID serviceId, UUID artistId) {
    if (customerRepository.findByTenantIdAndId(tenantId, customerId).isEmpty())
      throw new SecurityException("Appointment reference is outside actor tenant");
    if (serviceRepository.findById(serviceId).isEmpty()) {
      throw new IllegalArgumentException("Service not found: " + serviceId);
    }
    if (artistRepository.findById(artistId).isEmpty()) {
      throw new IllegalArgumentException("Artist not found: " + artistId);
    }
    if (serviceRepository
            .findByTenantIdAndStatus(tenantId, com.emme.services.domain.model.ServiceStatus.ACTIVE)
            .stream()
            .noneMatch(s -> s.getId().equals(serviceId))
        || artistRepository.findByTenantId(tenantId).stream()
            .noneMatch(a -> a.getId().equals(artistId)))
      throw new SecurityException("Appointment reference is outside actor tenant");
  }

  void ensureAvailable(UUID tenantId, UUID artistId, Instant startsAt, Instant endsAt) {
    ensureAvailable(tenantId, artistId, startsAt, endsAt, null);
  }

  void ensureAvailable(
      UUID tenantId, UUID artistId, Instant startsAt, Instant endsAt, UUID excludedAppointmentId) {
    if (collisionPort.hasCollision(tenantId, artistId, startsAt, endsAt, excludedAppointmentId)) {
      throw new IllegalStateException(
          "Slot conflict: artist "
              + artistId
              + " already has a confirmed appointment in this time range");
    }
  }

  AppointmentDetails toDetails(Appointment appointment) {
    String customerName =
        customerRepository
            .findByTenantIdAndId(appointment.getTenantId(), appointment.getCustomerId())
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
