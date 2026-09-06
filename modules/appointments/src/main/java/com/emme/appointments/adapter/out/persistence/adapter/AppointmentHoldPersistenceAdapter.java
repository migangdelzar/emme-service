package com.emme.appointments.adapter.out.persistence.adapter;

import com.emme.ai.contracts.appointment.AppointmentHold;
import com.emme.appointments.adapter.out.persistence.entity.AppointmentHoldEntity;
import com.emme.appointments.adapter.out.persistence.mapper.AppointmentHoldPersistenceMapper;
import com.emme.appointments.adapter.out.persistence.repository.SpringDataAppointmentHoldRepository;
import com.emme.appointments.application.port.out.AppointmentHoldRepository;
import com.emme.kernel.context.TenantContextHolder;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Persists holds in the tenant schema selected for the current connection. */
@Component
public final class AppointmentHoldPersistenceAdapter implements AppointmentHoldRepository {

  private final SpringDataAppointmentHoldRepository repository;
  private final AppointmentHoldPersistenceMapper mapper;

  public AppointmentHoldPersistenceAdapter(
      SpringDataAppointmentHoldRepository repository, AppointmentHoldPersistenceMapper mapper) {
    this.repository = Objects.requireNonNull(repository, "repository must not be null");
    this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
  }

  @Override
  public Optional<AppointmentHold> findById(UUID holdId) {
    return repository
        .findById(Objects.requireNonNull(holdId, "holdId must not be null"))
        .map(mapper::toDomain);
  }

  @Override
  public Optional<AppointmentHold> findByIdempotencyKey(String idempotencyKey) {
    return repository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
  }

  @Override
  public AppointmentHold save(AppointmentHold hold) {
    AppointmentHoldEntity entity =
        repository
            .findById(hold.holdId())
            .orElseGet(
                () -> mapper.toNewEntity(hold, TenantContextHolder.requireCurrentTenantId()));
    mapper.updateEntity(hold, entity);
    return mapper.toDomain(repository.save(entity));
  }

  @Override
  public void deleteById(UUID holdId) {
    repository.deleteById(Objects.requireNonNull(holdId, "holdId must not be null"));
  }
}
