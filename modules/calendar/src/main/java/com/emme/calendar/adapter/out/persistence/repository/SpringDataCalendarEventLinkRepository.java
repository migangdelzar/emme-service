package com.emme.calendar.adapter.out.persistence.repository;

import com.emme.calendar.adapter.out.persistence.entity.CalendarEventLinkEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataCalendarEventLinkRepository
    extends JpaRepository<CalendarEventLinkEntity, UUID> {

  Optional<CalendarEventLinkEntity> findByTenantIdAndAppointmentId(
      UUID tenantId, UUID appointmentId);

  List<CalendarEventLinkEntity> findByAppointmentId(UUID appointmentId);
}
