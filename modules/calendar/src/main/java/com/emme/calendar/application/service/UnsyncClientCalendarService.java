package com.emme.calendar.application.service;

import com.emme.calendar.api.command.UnsyncClientCalendarCommand;
import com.emme.calendar.api.usecase.UnsyncClientCalendarUseCase;
import com.emme.calendar.application.port.out.ClientCalendarSyncPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates removal of one client appointment event. */
@Service
@Transactional
@RequiredArgsConstructor
public class UnsyncClientCalendarService implements UnsyncClientCalendarUseCase {

  private final ClientCalendarSyncPort clientCalendarSyncPort;

  @Override
  public void unsync(UnsyncClientCalendarCommand command) {
    clientCalendarSyncPort.unsync(command.tenantId(), command.appointmentId(), command.userId());
  }
}
