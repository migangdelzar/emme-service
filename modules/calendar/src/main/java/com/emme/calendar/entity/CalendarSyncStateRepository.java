package com.emme.calendar.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarSyncStateRepository extends JpaRepository<CalendarSyncState, UUID> {

  List<CalendarSyncState> findByTenantId(UUID tenantId);

  Optional<CalendarSyncState> findByTenantIdAndProvider(UUID tenantId, CalendarProvider provider);
}
