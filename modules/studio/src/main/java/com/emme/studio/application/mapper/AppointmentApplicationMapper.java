package com.emme.studio.application.mapper;

import com.emme.studio.api.result.AppointmentDetails;
import com.emme.studio.domain.model.Appointment;

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
        appointment.getStatus().name());
  }
}
