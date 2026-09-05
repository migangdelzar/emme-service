package com.emme.calendar.adapter.out.persistence.repository;

import com.emme.calendar.adapter.out.persistence.entity.CalendarSyncStateEntity;
import com.emme.calendar.domain.model.CalendarProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataCalendarSyncStateRepository
    extends JpaRepository<CalendarSyncStateEntity, UUID> {

  Optional<CalendarSyncStateEntity> findByTenantIdAndProvider(
      UUID tenantId, CalendarProvider provider);
}
