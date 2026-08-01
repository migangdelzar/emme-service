package com.emme.calendar.application.service;

import com.emme.calendar.api.result.CalendarEventLinkInfo;
import com.emme.calendar.api.usecase.CalendarSyncApi;
import com.emme.calendar.application.port.out.CalendarEventLinkRepository;
import com.emme.calendar.domain.model.CalendarEventLink;
import com.emme.calendar.domain.model.CalendarProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class CalendarSyncApiService implements CalendarSyncApi {

  private final CalendarEventLinkRepository repo;

  CalendarSyncApiService(CalendarEventLinkRepository repo) {
    this.repo = repo;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CalendarEventLinkInfo> findByAppointmentId(UUID appointmentId) {
    return repo.findByAppointmentId(appointmentId).stream().map(this::toInfo).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<CalendarEventLinkInfo> findByTenantIdAndAppointmentId(
      UUID tenantId, UUID appointmentId) {
    return repo.findByTenantIdAndAppointmentId(tenantId, appointmentId).map(this::toInfo);
  }

  @Override
  public CalendarEventLinkInfo createLink(
      UUID tenantId, UUID appointmentId, String provider, String externalEventId) {
    CalendarProvider cp = CalendarProvider.valueOf(provider);
    var link = CalendarEventLink.pending(tenantId, appointmentId, cp, externalEventId);
    return toInfo(repo.save(link));
  }

  @Override
  public CalendarEventLinkInfo markSynced(UUID tenantId, UUID appointmentId, String etag) {
    var link =
        repo.findByTenantIdAndAppointmentId(tenantId, appointmentId)
            .orElseThrow(() -> new IllegalArgumentException("No link found for " + appointmentId));
    link.markSynced(etag);
    return toInfo(repo.save(link));
  }

  @Override
  public void markDeleted(UUID tenantId, UUID appointmentId) {
    var links = repo.findByAppointmentId(appointmentId);
    links.forEach(CalendarEventLink::markDeleted);
    repo.saveAll(links);
  }

  @Override
  public void markFailed(UUID tenantId, UUID appointmentId) {
    var links = repo.findByAppointmentId(appointmentId);
    links.forEach(CalendarEventLink::markFailed);
    repo.saveAll(links);
  }

  private CalendarEventLinkInfo toInfo(CalendarEventLink link) {
    return new CalendarEventLinkInfo(
        link.id(),
        link.appointmentId(),
        link.provider().name(),
        link.externalEventId(),
        link.etag(),
        link.status().name());
  }
}
