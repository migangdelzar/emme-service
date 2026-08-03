package com.emme.calendar.application.service;

import com.emme.calendar.api.command.SyncClientCalendarCommand;
import com.emme.calendar.api.result.ClientCalendarSyncDetails;
import com.emme.calendar.api.usecase.SyncClientCalendarUseCase;
import com.emme.calendar.application.port.out.ClientCalendarSyncPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates synchronization of one client appointment. */
@Service
@Transactional
public class SyncClientCalendarService implements SyncClientCalendarUseCase {

  private final ClientCalendarSyncPort clientCalendarSyncPort;

  public SyncClientCalendarService(ClientCalendarSyncPort clientCalendarSyncPort) {
    this.clientCalendarSyncPort = clientCalendarSyncPort;
  }

  @Override
  public ClientCalendarSyncDetails sync(SyncClientCalendarCommand command) {
    String eventId =
        clientCalendarSyncPort.sync(
            command.tenantId(),
            command.appointmentId(),
            command.userId(),
            command.startsAt(),
            command.endsAt(),
            command.summary(),
            command.description());
    return new ClientCalendarSyncDetails("synced", eventId);
  }
}
