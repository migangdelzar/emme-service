package com.emme.studio.adapter.out.persistence.repository;

import com.emme.studio.adapter.out.persistence.entity.AppointmentEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataAppointmentRepository extends JpaRepository<AppointmentEntity, UUID> {
  List<AppointmentEntity> findByTenantId(UUID tenantId);

  List<AppointmentEntity> findByTenantIdOrderByStartsAtDesc(UUID tenantId);

  List<AppointmentEntity> findByArtistIdAndStartsAtBetween(
      UUID artistId, Instant start, Instant end);

  List<AppointmentEntity> findByTenantIdAndStartsAtBetween(
      UUID tenantId, Instant start, Instant end);

  List<AppointmentEntity> findByCustomerId(UUID customerId);
}
