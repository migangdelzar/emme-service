package com.emme.appointments.adapter.out.persistence.repository;

import com.emme.appointments.adapter.out.persistence.entity.AppointmentEntity;
import com.emme.appointments.domain.model.AppointmentStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataAppointmentRepository extends JpaRepository<AppointmentEntity, UUID> {
  List<AppointmentEntity> findByTenantId(UUID tenantId);

  List<AppointmentEntity> findByTenantIdOrderByStartsAtDesc(UUID tenantId);

  boolean existsByTenantIdAndArtistIdAndStartsAtLessThanAndEndsAtGreaterThanAndStatusIn(
      UUID tenantId,
      UUID artistId,
      Instant end,
      Instant start,
      Collection<AppointmentStatus> statuses);

  boolean existsByTenantIdAndArtistIdAndStartsAtLessThanAndEndsAtGreaterThanAndStatusInAndIdNot(
      UUID tenantId,
      UUID artistId,
      Instant end,
      Instant start,
      Collection<AppointmentStatus> statuses,
      UUID excludedId);

  List<AppointmentEntity> findByTenantIdAndStartsAtBetween(
      UUID tenantId, Instant start, Instant end);

  List<AppointmentEntity> findByCustomerId(UUID customerId);
}
