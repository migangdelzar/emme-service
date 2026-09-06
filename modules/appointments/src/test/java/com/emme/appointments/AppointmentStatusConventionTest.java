package com.emme.appointments;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.appointments.api.result.AppointmentDetails;
import com.emme.appointments.api.result.AppointmentSummary;
import com.emme.appointments.domain.model.AppointmentStatus;
import org.junit.jupiter.api.Test;

class AppointmentStatusConventionTest {

  @Test
  void appointmentStatusUsesTheDomainEnumAcrossPublicBoundaries() {
    assertThat(AppointmentDetails.class.getRecordComponents()[9].getType())
        .isEqualTo(AppointmentStatus.class);
    assertThat(AppointmentSummary.class.getRecordComponents()[6].getType())
        .isEqualTo(AppointmentStatus.class);
  }
}
