package com.emme.appointments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.appointment.AppointmentHold;
import com.emme.appointments.api.command.CreateAppointmentHoldCommand;
import com.emme.appointments.application.port.out.AppointmentHoldRepository;
import com.emme.appointments.application.port.out.AppointmentRepository;
import com.emme.appointments.application.service.CreateAppointmentHoldService;
import com.emme.appointments.domain.model.Appointment;
import com.emme.appointments.domain.model.AppointmentStatus;
import com.emme.appointments.domain.model.ExternalCalendarStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppointmentHoldApplicationBoundaryTest {

  @Test
  void exposesHoldCommandsUseCasesAndServicesInTheAppointmentsModule() {
    Path root = sourcePath("modules/appointments/src/main/java/com/emme/appointments");

    assertThat(Files.exists(root.resolve("api/command/CreateAppointmentHoldCommand.java")))
        .isTrue();
    assertThat(Files.exists(root.resolve("api/usecase/CreateAppointmentHoldUseCase.java")))
        .isTrue();
    assertThat(Files.exists(root.resolve("api/usecase/ReleaseAppointmentHoldUseCase.java")))
        .isTrue();
  }

  @Test
  void createsAnIdempotentHoldWithTheConfiguredExpiry() {
    UUID appointmentId = UUID.randomUUID();
    Instant now = Instant.parse("2030-01-01T09:00:00Z");
    AppointmentRepository appointments = mock(AppointmentRepository.class);
    AppointmentHoldRepository holds = mock(AppointmentHoldRepository.class);
    when(appointments.findById(appointmentId))
        .thenReturn(
            Optional.of(
                Appointment.reconstitute(
                    appointmentId,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    now.plusSeconds(3600),
                    now.plusSeconds(7200),
                    AppointmentStatus.CONFIRMED,
                    ExternalCalendarStatus.NOT_SYNCED)));
    when(holds.findByIdempotencyKey("hold-1")).thenReturn(Optional.empty());
    when(holds.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    AppointmentHold hold =
        new CreateAppointmentHoldService(
                appointments, holds, Clock.fixed(now, ZoneOffset.UTC), Duration.ofMinutes(15))
            .create(new CreateAppointmentHoldCommand(appointmentId, "hold-1"));

    assertThat(hold.appointmentId()).isEqualTo(appointmentId);
    assertThat(hold.expiresAt()).isEqualTo(now.plus(Duration.ofMinutes(15)));
    verify(holds).save(any());
  }

  @Test
  void reusesAnExistingHoldBeforeReadingTheAppointment() {
    UUID appointmentId = UUID.randomUUID();
    AppointmentHold existing =
        new AppointmentHold(
            UUID.randomUUID(), appointmentId, Instant.parse("2030-01-01T09:15:00Z"), "hold-1");
    AppointmentHoldRepository holds = mock(AppointmentHoldRepository.class);
    AppointmentRepository appointments = mock(AppointmentRepository.class);
    when(holds.findByIdempotencyKey("hold-1")).thenReturn(Optional.of(existing));

    AppointmentHold actual =
        new CreateAppointmentHoldService(
                appointments,
                holds,
                Clock.fixed(Instant.parse("2030-01-01T09:00:00Z"), ZoneOffset.UTC),
                Duration.ofMinutes(15))
            .create(new CreateAppointmentHoldCommand(appointmentId, "hold-1"));

    assertThat(actual).isEqualTo(existing);
    verify(appointments, never()).findById(appointmentId);
    verify(holds, never()).save(any());
    verifyNoInteractions(appointments);
  }

  private static Path sourcePath(String relativePath) {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve(relativePath);
      if (Files.exists(candidate)) return candidate;
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate source path: " + relativePath);
  }
}
