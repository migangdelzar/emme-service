package com.emme.calendar.application.service;

import com.emme.calendar.api.query.GetGoogleOAuthStatusQuery;
import com.emme.calendar.api.result.GoogleOAuthConnectionStatus;
import com.emme.calendar.api.usecase.GetGoogleOAuthStatusUseCase;
import com.emme.calendar.application.port.out.GoogleOAuthPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates reads of Google OAuth connection state. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetGoogleOAuthStatusService implements GetGoogleOAuthStatusUseCase {

  private final GoogleOAuthPort googleOAuthPort;

  @Override
  public GoogleOAuthConnectionStatus get(GetGoogleOAuthStatusQuery query) {
    return new GoogleOAuthConnectionStatus(
        googleOAuthPort.isConnected(query.tenantId(), query.userId(), query.persona()));
  }
}
