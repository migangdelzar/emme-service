package com.emme.appointments.application.mapper;

import com.emme.appointments.api.result.AppointmentDetails;
import com.emme.appointments.api.type.AppointmentStatus;
import com.emme.appointments.domain.model.Appointment;

/** Maps Studio domain aggregates to public application read models. */
public final class AppointmentApplicationMapper {

  private AppointmentApplicationMapper() {}

  public static AppointmentDetails toDetails(
      Appointment appointment, String customerName, String serviceName, String artistName) {
    return new AppointmentDetails(
        appointment.getId(),
        appointment.getCustomerId(),
        customerName,
        appointment.getServiceId(),
        serviceName,
        appointment.getArtistId(),
        artistName,
        appointment.getStartsAt(),
        appointment.getEndsAt(),
        AppointmentStatus.valueOf(appointment.getStatus().name()));
  }
}
