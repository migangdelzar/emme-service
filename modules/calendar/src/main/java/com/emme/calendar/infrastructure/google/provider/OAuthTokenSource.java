package com.emme.calendar.infrastructure.google.provider;

import com.emme.calendar.api.TokenSource;
import com.emme.calendar.infrastructure.google.application.GoogleOAuthService;
import com.emme.calendar.infrastructure.google.entity.PersonaType;
import com.emme.identity.UserContextHolder;
import com.emme.kernel.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * User OAuth implementation of TokenSource. Reads tokens from the google_oauth_token table via
 * GoogleOAuthService. Auto-injected into GoogleCalendarClient when the google module is present.
 */
@Component
public class OAuthTokenSource implements TokenSource {

  private static final Logger log = LoggerFactory.getLogger(OAuthTokenSource.class);
  private final GoogleOAuthService oauthService;

  public OAuthTokenSource(GoogleOAuthService oauthService) {
    this.oauthService = oauthService;
  }

  @Override
  public String getAccessToken() {
    var tenantId = TenantContext.getCurrentTenantId();
    var userId = UserContextHolder.currentSubject();
    return oauthService.getValidAccessToken(tenantId, userId, PersonaType.STAFF);
  }

  @Override
  public boolean isConfigured() {
    try {
      var tenantId = TenantContext.getCurrentTenantId();
      var userId = UserContextHolder.currentSubject();
      return oauthService.isConnected(tenantId, userId, PersonaType.STAFF);
    } catch (Exception e) {
      log.debug("OAuth token source not available: {}", e.getMessage());
      return false;
    }
  }
}
