package com.emme.calendar.adapter.out.persistence.mapper;

import com.emme.calendar.adapter.out.persistence.entity.CalendarEventLinkEntity;
import com.emme.calendar.adapter.out.persistence.entity.CalendarSyncStateEntity;
import com.emme.calendar.domain.model.CalendarEventLink;
import com.emme.calendar.domain.model.CalendarSyncState;
import org.springframework.stereotype.Component;

/** Translates Calendar domain state to and from database representations. */
@Component
public final class CalendarPersistenceMapper {

  public CalendarEventLink toDomain(CalendarEventLinkEntity entity) {
    return CalendarEventLink.restore(
        entity.getId(),
        entity.getTenantId(),
        entity.getAppointmentId(),
        entity.getProvider(),
        entity.getExternalEventId(),
        entity.getEtag(),
        entity.getStatus());
  }

  public CalendarEventLinkEntity toEntity(CalendarEventLink domain) {
    CalendarEventLinkEntity entity =
        CalendarEventLinkEntity.restore(
            domain.id(),
            domain.tenantId(),
            domain.appointmentId(),
            domain.provider(),
            domain.externalEventId(),
            domain.etag(),
            domain.status());
    return entity;
  }

  public CalendarSyncState toDomain(CalendarSyncStateEntity entity) {
    return CalendarSyncState.restore(
        entity.getId(),
        entity.getTenantId(),
        entity.getProvider(),
        entity.getSyncToken(),
        entity.getLastSyncedAt(),
        entity.getStatus());
  }

  public CalendarSyncStateEntity toEntity(CalendarSyncState domain) {
    return CalendarSyncStateEntity.restore(
        domain.id(),
        domain.tenantId(),
        domain.provider(),
        domain.syncToken(),
        domain.lastSyncedAt(),
        domain.status());
  }
}
