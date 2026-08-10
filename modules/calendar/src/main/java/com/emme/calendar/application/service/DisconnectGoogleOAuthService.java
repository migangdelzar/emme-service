package com.emme.calendar.application.service;

import com.emme.calendar.api.command.DisconnectGoogleOAuthCommand;
import com.emme.calendar.api.usecase.DisconnectGoogleOAuthUseCase;
import com.emme.calendar.application.port.out.GoogleOAuthPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates revocation and removal of Google OAuth credentials. */
@Service
@Transactional
public class DisconnectGoogleOAuthService implements DisconnectGoogleOAuthUseCase {

  private final GoogleOAuthPort googleOAuthPort;

  public DisconnectGoogleOAuthService(GoogleOAuthPort googleOAuthPort) {
    this.googleOAuthPort = googleOAuthPort;
  }

  @Override
  public void disconnect(DisconnectGoogleOAuthCommand command) {
    googleOAuthPort.revokeToken(command.tenantId(), command.userId(), command.persona());
  }
}
