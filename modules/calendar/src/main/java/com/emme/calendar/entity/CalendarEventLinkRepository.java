package com.emme.calendar.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarEventLinkRepository extends JpaRepository<CalendarEventLink, UUID> {

  List<CalendarEventLink> findByTenantId(UUID tenantId);

  Optional<CalendarEventLink> findByTenantIdAndAppointmentId(UUID tenantId, UUID appointmentId);

  List<CalendarEventLink> findByAppointmentId(UUID appointmentId);
}
