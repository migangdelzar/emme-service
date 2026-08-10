package com.emme.calendar.application.port.out;

import com.emme.calendar.api.type.GoogleOAuthPersona;
import java.util.UUID;

/** Outbound capability required by Calendar OAuth workflows. */
public interface GoogleOAuthPort {

  String buildAuthorizationUrl(GoogleOAuthPersona persona, String state);

  GoogleOAuthTokens exchangeCode(String code);

  void storeToken(
      UUID tenantId, String userId, GoogleOAuthPersona persona, GoogleOAuthTokens tokens);

  boolean isConnected(UUID tenantId, String userId, GoogleOAuthPersona persona);

  void revokeToken(UUID tenantId, String userId, GoogleOAuthPersona persona);
}
