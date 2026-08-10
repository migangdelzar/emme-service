package com.emme.calendar.api.usecase;

import com.emme.calendar.api.command.CompleteGoogleOAuthCommand;

/** Exchanges a Google authorization code and stores the resulting credentials. */
public interface CompleteGoogleOAuthUseCase {

  void complete(CompleteGoogleOAuthCommand command);
}
