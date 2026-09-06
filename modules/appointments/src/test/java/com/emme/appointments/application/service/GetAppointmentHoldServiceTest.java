package com.emme.appointments.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.appointment.AppointmentHold;
import com.emme.appointments.application.port.out.AppointmentHoldRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetAppointmentHoldServiceTest {

  @Test
  void readsAHoldThroughThePublicAppointmentQueryBoundary() {
    UUID holdId = UUID.randomUUID();
    AppointmentHold hold =
        new AppointmentHold(
            holdId, UUID.randomUUID(), Instant.parse("2030-01-01T10:15:00Z"), "hold-1");
    AppointmentHoldRepository repository = mock(AppointmentHoldRepository.class);
    when(repository.findById(holdId)).thenReturn(Optional.of(hold));

    var service = new GetAppointmentHoldService(repository);

    assertThat(service.get(holdId)).contains(hold);
  }
}
