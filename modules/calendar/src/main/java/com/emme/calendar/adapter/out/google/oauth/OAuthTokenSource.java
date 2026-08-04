package com.emme.calendar.adapter.out.google.oauth;

import com.emme.calendar.adapter.out.google.adapter.GoogleOAuthAdapter;
import com.emme.calendar.adapter.out.google.model.PersonaType;
import com.emme.kernel.context.TenantContext;
import com.emme.shared.web.security.CurrentUserContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * User OAuth implementation of GoogleUserTokenSource. Reads tokens from the google_oauth_token
 * table via GoogleOAuthService. Auto-injected into GoogleCalendarClient when the google module is
 * present.
 */
@Component
public class OAuthTokenSource implements GoogleUserTokenSource {

  private static final Logger log = LoggerFactory.getLogger(OAuthTokenSource.class);
  private final GoogleOAuthAdapter oauthService;

  public OAuthTokenSource(GoogleOAuthAdapter oauthService) {
    this.oauthService = oauthService;
  }

  @Override
  public String getAccessToken() {
    var tenantId = TenantContext.getCurrentTenantId();
    var userId = CurrentUserContextHolder.currentSubject();
    return oauthService.getValidAccessToken(tenantId, userId, PersonaType.STAFF);
  }

  @Override
  public boolean isConfigured() {
    try {
      var tenantId = TenantContext.getCurrentTenantId();
      var userId = CurrentUserContextHolder.currentSubject();
      return oauthService.isConnected(tenantId, userId, PersonaType.STAFF);
    } catch (Exception e) {
      log.debug("OAuth token source not available: {}", e.getMessage());
      return false;
    }
  }
}
