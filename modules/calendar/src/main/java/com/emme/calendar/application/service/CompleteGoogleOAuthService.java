package com.emme.calendar.application.service;

import com.emme.calendar.api.command.CompleteGoogleOAuthCommand;
import com.emme.calendar.api.usecase.CompleteGoogleOAuthUseCase;
import com.emme.calendar.application.port.out.GoogleOAuthPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates code exchange and credential persistence for Google OAuth. */
@Service
@Transactional
public class CompleteGoogleOAuthService implements CompleteGoogleOAuthUseCase {

  private final GoogleOAuthPort googleOAuthPort;

  public CompleteGoogleOAuthService(GoogleOAuthPort googleOAuthPort) {
    this.googleOAuthPort = googleOAuthPort;
  }

  @Override
  public void complete(CompleteGoogleOAuthCommand command) {
    googleOAuthPort.storeToken(
        command.tenantId(),
        command.userId(),
        command.persona(),
        googleOAuthPort.exchangeCode(command.code()));
  }
}
