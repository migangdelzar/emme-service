package com.emme.calendar.api.usecase;

import com.emme.calendar.api.command.UnsyncClientCalendarCommand;

/** Removes a client appointment event from Google Calendar. */
public interface UnsyncClientCalendarUseCase {

  void unsync(UnsyncClientCalendarCommand command);
}
