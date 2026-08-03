package com.emme.studio.adapter.out.persistence.adapter;

import com.emme.studio.adapter.out.persistence.entity.AppointmentEntity;
import com.emme.studio.adapter.out.persistence.entity.ArtistEntity;
import com.emme.studio.adapter.out.persistence.entity.CustomerEntity;
import com.emme.studio.adapter.out.persistence.entity.ServiceEntity;
import com.emme.studio.adapter.out.persistence.mapper.AppointmentPersistenceMapper;
import com.emme.studio.adapter.out.persistence.repository.SpringDataAppointmentRepository;
import com.emme.studio.adapter.out.persistence.repository.SpringDataArtistRepository;
import com.emme.studio.adapter.out.persistence.repository.SpringDataCustomerRepository;
import com.emme.studio.adapter.out.persistence.repository.SpringDataServiceRepository;
import com.emme.studio.application.port.out.AppointmentRepository;
import com.emme.studio.domain.model.Appointment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implements the appointment persistence port using Spring Data JPA. */
@Component
public class AppointmentPersistenceAdapter implements AppointmentRepository {

  private final SpringDataAppointmentRepository repository;
  private final SpringDataCustomerRepository customerRepository;
  private final SpringDataServiceRepository serviceRepository;
  private final SpringDataArtistRepository artistRepository;
  private final AppointmentPersistenceMapper mapper;

  public AppointmentPersistenceAdapter(
      SpringDataAppointmentRepository repository,
      SpringDataCustomerRepository customerRepository,
      SpringDataServiceRepository serviceRepository,
      SpringDataArtistRepository artistRepository) {
    this.repository = repository;
    this.customerRepository = customerRepository;
    this.serviceRepository = serviceRepository;
    this.artistRepository = artistRepository;
    this.mapper = new AppointmentPersistenceMapper();
  }

  @Override
  public Appointment save(Appointment appointment) {
    AppointmentEntity entity;
    if (appointment.getId() == null) {
      CustomerEntity customer = customerRepository.getReferenceById(appointment.getCustomerId());
      ServiceEntity service = serviceRepository.getReferenceById(appointment.getServiceId());
      ArtistEntity artist = artistRepository.getReferenceById(appointment.getArtistId());
      entity = mapper.toNewEntity(appointment, customer, service, artist);
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
