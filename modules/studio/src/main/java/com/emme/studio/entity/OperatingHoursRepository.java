package com.emme.studio.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperatingHoursRepository extends JpaRepository<OperatingHours, UUID> {
  List<OperatingHours> findByTenantId(UUID tenantId);

  Optional<OperatingHours> findByTenantIdAndDayOfWeek(UUID tenantId, DayOfWeek dayOfWeek);
}
