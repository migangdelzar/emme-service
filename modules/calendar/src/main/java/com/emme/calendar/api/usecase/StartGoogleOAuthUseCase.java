package com.emme.calendar.api.usecase;

import com.emme.calendar.api.command.StartGoogleOAuthCommand;
import com.emme.calendar.api.result.GoogleAuthorizationUrl;

/** Starts a Google OAuth consent flow. */
public interface StartGoogleOAuthUseCase {

  GoogleAuthorizationUrl start(StartGoogleOAuthCommand command);
}
