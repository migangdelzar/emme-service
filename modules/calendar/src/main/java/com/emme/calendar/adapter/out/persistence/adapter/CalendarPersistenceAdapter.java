package com.emme.calendar.adapter.out.persistence.adapter;

import com.emme.calendar.adapter.out.persistence.entity.CalendarEventLinkEntity;
import com.emme.calendar.adapter.out.persistence.entity.CalendarSyncStateEntity;
import com.emme.calendar.adapter.out.persistence.mapper.CalendarPersistenceMapper;
import com.emme.calendar.adapter.out.persistence.repository.SpringDataCalendarEventLinkRepository;
import com.emme.calendar.adapter.out.persistence.repository.SpringDataCalendarSyncStateRepository;
import com.emme.calendar.application.port.out.CalendarEventLinkRepository;
import com.emme.calendar.application.port.out.CalendarSyncStateRepository;
import com.emme.calendar.domain.model.CalendarEventLink;
import com.emme.calendar.domain.model.CalendarProvider;
import com.emme.calendar.domain.model.CalendarSyncState;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implements Calendar application persistence ports using Spring Data repositories. */
@Component
public class CalendarPersistenceAdapter
    implements CalendarEventLinkRepository, CalendarSyncStateRepository {

  private final SpringDataCalendarEventLinkRepository eventLinks;
  private final SpringDataCalendarSyncStateRepository syncStates;
  private final CalendarPersistenceMapper mapper;

  public CalendarPersistenceAdapter(
      SpringDataCalendarEventLinkRepository eventLinks,
      SpringDataCalendarSyncStateRepository syncStates,
      CalendarPersistenceMapper mapper) {
    this.eventLinks = eventLinks;
    this.syncStates = syncStates;
    this.mapper = mapper;
  }

  @Override
  public List<CalendarEventLink> findByAppointmentId(UUID appointmentId) {
    return eventLinks.findByAppointmentId(appointmentId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public Optional<CalendarEventLink> findByTenantIdAndAppointmentId(
      UUID tenantId, UUID appointmentId) {
    return eventLinks.findByTenantIdAndAppointmentId(tenantId, appointmentId).map(mapper::toDomain);
  }

  @Override
  public CalendarEventLink save(CalendarEventLink link) {
    CalendarEventLinkEntity entity =
        eventLinks.findById(link.id()).orElseGet(() -> mapper.toEntity(link));
    entity.setEtag(link.etag());
    entity.setStatus(link.status());
    return mapper.toDomain(eventLinks.save(entity));
  }

  @Override
  public List<CalendarEventLink> saveAll(List<CalendarEventLink> links) {
    return links.stream().map(this::save).toList();
  }

  @Override
  public Optional<CalendarSyncState> findByTenantIdAndProvider(
      UUID tenantId, CalendarProvider provider) {
    return syncStates.findByTenantIdAndProvider(tenantId, provider).map(mapper::toDomain);
  }

  @Override
  public CalendarSyncState save(CalendarSyncState state) {
    CalendarSyncStateEntity entity =
        syncStates.findById(state.id()).orElseGet(() -> mapper.toEntity(state));
    entity.setSyncToken(state.syncToken());
    entity.setLastSyncedAt(state.lastSyncedAt());
    entity.setStatus(state.status());
    return mapper.toDomain(syncStates.save(entity));
  }
}
