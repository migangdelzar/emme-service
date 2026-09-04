package com.emme.appointments.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.appointments.api.command.CancelAppointmentCommand;
import com.emme.appointments.api.command.RescheduleAppointmentCommand;
import com.emme.appointments.api.type.AppointmentActor;
import com.emme.appointments.application.port.out.AppointmentCollisionPort;
import com.emme.appointments.application.port.out.AppointmentEventPublisher;
import com.emme.appointments.application.port.out.AppointmentRepository;
import com.emme.appointments.domain.model.Appointment;
import com.emme.appointments.domain.model.AppointmentStatus;
import com.emme.appointments.domain.model.ExternalCalendarStatus;
import com.emme.clients.application.port.out.CustomerRepository;
import com.emme.services.application.port.out.ArtistRepository;
import com.emme.services.application.port.out.ServiceRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppointmentMutationAuthorizationTest {
  private static final UUID TENANT = UUID.randomUUID();
  private static final UUID PRINCIPAL = UUID.randomUUID();
  private final AppointmentRepository appointments = mock(AppointmentRepository.class);
  private final AppointmentCollisionPort collisions = mock(AppointmentCollisionPort.class);
  private final CustomerRepository customers = mock(CustomerRepository.class);
  private final ServiceRepository services = mock(ServiceRepository.class);
  private final ArtistRepository artists = mock(ArtistRepository.class);
  private final AppointmentEventPublisher events = mock(AppointmentEventPublisher.class);

  @Test
  void rejectsBookingForAnotherTenantAndForCrossTenantReferences() {
    UUID customer = UUID.randomUUID();
    UUID service = UUID.randomUUID();
    UUID artist = UUID.randomUUID();
    when(customers.findByTenantIdAndId(TENANT, customer)).thenReturn(Optional.empty());
    when(services.findById(service))
        .thenReturn(Optional.of(mock(com.emme.services.domain.model.Service.class)));
    when(artists.findById(artist))
        .thenReturn(Optional.of(mock(com.emme.services.domain.model.Artist.class)));

    CreateAppointmentService serviceUnderTest = createService();
    assertThatThrownBy(() -> serviceUnderTest.create(TENANT, customer, service, artist, START, END))
        .isInstanceOf(SecurityException.class);
  }

  @Test
  void rejectsClientMutationWhenPrincipalDoesNotOwnTheCustomerAppointment() {
    UUID appointmentId = UUID.randomUUID();
    Appointment appointment =
        appointment(appointmentId, TENANT, UUID.randomUUID(), AppointmentStatus.CONFIRMED);
    when(appointments.findById(appointmentId)).thenReturn(Optional.of(appointment));

    RescheduleAuthorizedAppointmentService serviceUnderTest = authorizedRescheduleService();
    assertThatThrownBy(
            () ->
                serviceUnderTest.reschedule(
                    new RescheduleAppointmentCommand(
                        new AppointmentActor(TENANT, PRINCIPAL, Set.of("client"), "key"),
                        appointmentId,
                        START,
                        END,
                        true)))
        .isInstanceOf(SecurityException.class);
  }

  @Test
  void rejectsNonConfirmedAppointmentMutation() {
    UUID appointmentId = UUID.randomUUID();
    Appointment appointment =
        appointment(appointmentId, TENANT, PRINCIPAL, AppointmentStatus.DRAFT);
    when(appointments.findById(appointmentId)).thenReturn(Optional.of(appointment));
    CancelAuthorizedAppointmentService serviceUnderTest = authorizedCancelService();

    assertThatThrownBy(
            () ->
                serviceUnderTest.cancel(
                    new CancelAppointmentCommand(
                        new AppointmentActor(TENANT, PRINCIPAL, Set.of("client"), "key"),
                        appointmentId,
                        true)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rescheduleExcludesTheAppointmentBeingMovedFromCollisionCheck() {
    UUID appointmentId = UUID.randomUUID();
    Appointment appointment =
        appointment(appointmentId, TENANT, PRINCIPAL, AppointmentStatus.CONFIRMED);
    when(appointments.findById(appointmentId)).thenReturn(Optional.of(appointment));
    when(collisions.hasCollision(TENANT, appointment.getArtistId(), START, END, appointmentId))
        .thenReturn(false);
    when(appointments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    authorizedRescheduleService()
        .reschedule(
            new RescheduleAppointmentCommand(
                new AppointmentActor(TENANT, PRINCIPAL, Set.of("client"), "key"),
                appointmentId,
                START,
                END,
                true));

    verify(collisions).hasCollision(TENANT, appointment.getArtistId(), START, END, appointmentId);
  }

  private CreateAppointmentService createService() {
    return new CreateAppointmentService(
        appointments, collisions, customers, services, artists, events);
  }

  private CancelAppointmentService cancelService() {
    return new CancelAppointmentService(
        appointments, collisions, customers, services, artists, events);
  }

  private CancelAuthorizedAppointmentService authorizedCancelService() {
    return new CancelAuthorizedAppointmentService(cancelService());
  }

  private RescheduleAppointmentService rescheduleService() {
    return new RescheduleAppointmentService(
        appointments, collisions, customers, services, artists, events);
  }

  private RescheduleAuthorizedAppointmentService authorizedRescheduleService() {
    return new RescheduleAuthorizedAppointmentService(rescheduleService());
  }

  private static final Instant START = Instant.parse("2030-01-01T10:00:00Z");
  private static final Instant END = Instant.parse("2030-01-01T11:00:00Z");

  private static Appointment appointment(
      UUID id, UUID tenant, UUID customer, AppointmentStatus status) {
    return Appointment.reconstitute(
        id,
        tenant,
        customer,
        UUID.randomUUID(),
        UUID.randomUUID(),
        START,
        END,
        status,
        ExternalCalendarStatus.NOT_SYNCED);
  }
}
