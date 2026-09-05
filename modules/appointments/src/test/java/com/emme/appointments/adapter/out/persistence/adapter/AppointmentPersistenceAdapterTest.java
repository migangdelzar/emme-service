package com.emme.appointments.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.appointments.adapter.out.persistence.repository.SpringDataAppointmentRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AppointmentPersistenceAdapterTest {

  @Test
  void listsAppointmentsInReverseStartOrderFromTheCurrentTenantSchema() {
    SpringDataAppointmentRepository repository = org.mockito.Mockito.mock();
    AppointmentPersistenceAdapter adapter = new AppointmentPersistenceAdapter(repository);
    when(repository.findAllByOrderByStartsAtDesc()).thenReturn(List.of());

    assertThat(adapter.findAllOrderByStartsAtDesc()).isEmpty();

    verify(repository).findAllByOrderByStartsAtDesc();
  }

  @Test
  void listsAppointmentsInTheRequestedDateRangeFromTheCurrentTenantSchema() {
    SpringDataAppointmentRepository repository = org.mockito.Mockito.mock();
    AppointmentPersistenceAdapter adapter = new AppointmentPersistenceAdapter(repository);
    Instant startsAt = Instant.parse("2026-01-01T00:00:00Z");
    Instant endsAt = Instant.parse("2026-01-02T00:00:00Z");
    when(repository.findByStartsAtBetween(startsAt, endsAt)).thenReturn(List.of());

    assertThat(adapter.findByStartsAtBetween(startsAt, endsAt)).isEmpty();

    verify(repository).findByStartsAtBetween(startsAt, endsAt);
  }
}
