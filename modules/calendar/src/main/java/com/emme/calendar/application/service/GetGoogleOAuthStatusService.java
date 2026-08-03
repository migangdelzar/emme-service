package com.emme.calendar.application.service;

import com.emme.calendar.api.command.GetGoogleOAuthStatusQuery;
import com.emme.calendar.api.result.GoogleOAuthConnectionStatus;
import com.emme.calendar.api.usecase.GetGoogleOAuthStatusUseCase;
import com.emme.calendar.application.port.out.GoogleOAuthPort;
import org.springframework.stereotype.Service;

/** Coordinates reads of Google OAuth connection state. */
@Service
public class GetGoogleOAuthStatusService implements GetGoogleOAuthStatusUseCase {

  private final GoogleOAuthPort googleOAuthPort;

  public GetGoogleOAuthStatusService(GoogleOAuthPort googleOAuthPort) {
    this.googleOAuthPort = googleOAuthPort;
  }

  @Override
  public GoogleOAuthConnectionStatus get(GetGoogleOAuthStatusQuery query) {
    return new GoogleOAuthConnectionStatus(
        googleOAuthPort.isConnected(query.tenantId(), query.userId(), query.persona()));
  }
}
