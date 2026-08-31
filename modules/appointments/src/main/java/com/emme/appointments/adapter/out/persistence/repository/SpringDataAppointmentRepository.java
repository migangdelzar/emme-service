package com.emme.appointments.adapter.out.persistence.repository;

import com.emme.appointments.adapter.out.persistence.entity.AppointmentEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataAppointmentRepository extends JpaRepository<AppointmentEntity, UUID> {
  List<AppointmentEntity> findByTenantId(UUID tenantId);

  List<AppointmentEntity> findByTenantIdOrderByStartsAtDesc(UUID tenantId);

  List<AppointmentEntity> findByArtistIdAndStartsAtLessThanAndEndsAtGreaterThan(
      UUID artistId, Instant end, Instant start);

  List<AppointmentEntity> findByTenantIdAndArtistIdAndStartsAtLessThanAndEndsAtGreaterThan(
      UUID tenantId, UUID artistId, Instant end, Instant start);

  List<AppointmentEntity> findByArtistIdAndStartsAtBetween(
      UUID artistId, Instant start, Instant end);

  List<AppointmentEntity> findByArtistIdAndStartsAtLessThanAndEndsAtGreaterThanAndIdNot(
      UUID artistId, Instant end, Instant start, UUID excludedId);

  List<AppointmentEntity> findByTenantIdAndStartsAtBetween(
      UUID tenantId, Instant start, Instant end);

  List<AppointmentEntity> findByCustomerId(UUID customerId);
}
