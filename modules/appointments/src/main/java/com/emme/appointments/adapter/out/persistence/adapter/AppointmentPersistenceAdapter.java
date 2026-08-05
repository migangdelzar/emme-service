package com.emme.appointments.adapter.out.persistence.adapter;

import com.emme.appointments.adapter.out.persistence.entity.AppointmentEntity;
import com.emme.appointments.adapter.out.persistence.mapper.AppointmentPersistenceMapper;
import com.emme.appointments.adapter.out.persistence.repository.SpringDataAppointmentRepository;
import com.emme.appointments.application.port.out.AppointmentRepository;
import com.emme.appointments.domain.model.Appointment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implements the appointment persistence port using Spring Data JPA. */
@Component
public class AppointmentPersistenceAdapter implements AppointmentRepository {

  private final SpringDataAppointmentRepository repository;
  private final AppointmentPersistenceMapper mapper;

  public AppointmentPersistenceAdapter(
      SpringDataAppointmentRepository repository) {
    this.repository = repository;
    this.mapper = new AppointmentPersistenceMapper();
  }

  @Override
  public Appointment save(Appointment appointment) {
    AppointmentEntity entity;
    if (appointment.getId() == null) {
      entity = mapper.toNewEntity(appointment);
    } else {
      entity = repository.findById(appointment.getId()).orElseThrow();
    }
    mapper.updateEntity(appointment, entity);
    return mapper.toDomain(repository.save(entity));
  }

  @Override
  public Optional<Appointment> findById(UUID id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<Appointment> findByTenantIdOrderByStartsAtDesc(UUID tenantId) {
    return repository.findByTenantIdOrderByStartsAtDesc(tenantId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Appointment> findByTenantIdAndStartsAtBetween(
      UUID tenantId, Instant startsAt, Instant endsAt) {
    return repository.findByTenantIdAndStartsAtBetween(tenantId, startsAt, endsAt).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Appointment> findByArtistIdAndStartsAtBetween(
      UUID artistId, Instant startsAt, Instant endsAt) {
    return repository.findByArtistIdAndStartsAtBetween(artistId, startsAt, endsAt).stream()
        .map(mapper::toDomain)
        .toList();
  }
}
