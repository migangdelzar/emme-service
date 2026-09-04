package com.emme.salon.adapter.out.persistence.repository;

import com.emme.salon.adapter.out.persistence.entity.OperatingHoursEntity;
import com.emme.salon.domain.model.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataOperatingHoursRepository
    extends JpaRepository<OperatingHoursEntity, UUID> {
  List<OperatingHoursEntity> findByTenantId(UUID tenantId);

  Optional<OperatingHoursEntity> findByTenantIdAndDayOfWeek(UUID tenantId, DayOfWeek dayOfWeek);

  Optional<OperatingHoursEntity> findByTenantIdAndId(UUID tenantId, UUID id);
}
