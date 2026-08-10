package com.emme.calendar.api.usecase;

import com.emme.calendar.api.command.SyncClientCalendarCommand;
import com.emme.calendar.api.result.ClientCalendarSyncDetails;

/** Synchronizes a client appointment with Google Calendar. */
public interface SyncClientCalendarUseCase {

  ClientCalendarSyncDetails sync(SyncClientCalendarCommand command);
}
