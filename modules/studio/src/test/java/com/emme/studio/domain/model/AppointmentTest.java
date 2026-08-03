package com.emme.studio.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppointmentTest {

  @Test
  void newAppointmentStartsConfirmedWithoutPersistenceIdentity() {
    Appointment appointment = appointment();

    assertThat(appointment.getId()).isNull();
    assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
  }

  @Test
  void appointmentLifecycleUsesExplicitBusinessTransitions() {
    Appointment appointment = appointment();

    appointment.start();
    appointment.complete();

    assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
  }

  @Test
  void appointmentRejectsInvalidLifecycleTransitions() {
    Appointment appointment = appointment();

    assertThatThrownBy(appointment::complete)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("IN_PROGRESS");
  }

  @Test
  void reschedulingUpdatesTheAppointmentInterval() {
    Appointment appointment = appointment();
    Instant newStart = Instant.parse("2026-08-01T12:00:00Z");
    Instant newEnd = Instant.parse("2026-08-01T13:00:00Z");

    appointment.reschedule(newStart, newEnd);

    assertThat(appointment.getStartsAt()).isEqualTo(newStart);
    assertThat(appointment.getEndsAt()).isEqualTo(newEnd);
  }

  private static Appointment appointment() {
    return new Appointment(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        Instant.parse("2026-08-01T10:00:00Z"),
        Instant.parse("2026-08-01T11:00:00Z"));
  }
}
