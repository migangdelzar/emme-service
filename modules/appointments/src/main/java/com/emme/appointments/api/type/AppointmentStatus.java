package com.emme.appointments.api.type;

/** Stable appointment lifecycle values exposed by the application API. */
public enum AppointmentStatus {
  DRAFT,
  CONFIRMED,
  IN_PROGRESS,
  COMPLETED,
  CANCELLED,
  NO_SHOW
}
