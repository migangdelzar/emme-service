package com.emme.studio.application.result;

import com.emme.studio.domain.model.Appointment;
import java.time.Instant;
import java.util.UUID;

/** Internal read model used by inbound adapters; it contains no persistence types. */
public record AppointmentView(
    UUID id,
    UUID customerId,
    String customerName,
    UUID serviceId,
    String serviceName,
    UUID artistId,
    String artistName,
    Instant startsAt,
    Instant endsAt,
    String status) {

  public static AppointmentView from(
      Appointment appointment, String customerName, String serviceName, String artistName) {
    return new AppointmentView(
        appointment.getId(),
        appointment.getCustomerId(),
        customerName,
        appointment.getServiceId(),
        serviceName,
        appointment.getArtistId(),
        artistName,
        appointment.getStartsAt(),
        appointment.getEndsAt(),
        appointment.getStatus().name());
  }
}
