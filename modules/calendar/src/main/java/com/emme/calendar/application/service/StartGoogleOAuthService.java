package com.emme.calendar.application.service;

import com.emme.calendar.api.command.StartGoogleOAuthCommand;
import com.emme.calendar.api.result.GoogleAuthorizationUrl;
import com.emme.calendar.api.usecase.StartGoogleOAuthUseCase;
import com.emme.calendar.application.port.out.GoogleOAuthPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Coordinates construction of a Google OAuth consent URL. */
@Service
@RequiredArgsConstructor
public class StartGoogleOAuthService implements StartGoogleOAuthUseCase {

  private final GoogleOAuthPort googleOAuthPort;

  @Override
  public GoogleAuthorizationUrl start(StartGoogleOAuthCommand command) {
    return new GoogleAuthorizationUrl(
        googleOAuthPort.buildAuthorizationUrl(command.persona(), command.state()));
  }
}
