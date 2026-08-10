package com.emme.calendar.api.usecase;

import com.emme.calendar.api.command.DisconnectGoogleOAuthCommand;

/** Revokes and removes Google OAuth credentials. */
public interface DisconnectGoogleOAuthUseCase {

  void disconnect(DisconnectGoogleOAuthCommand command);
}
