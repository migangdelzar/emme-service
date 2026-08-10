package com.emme.calendar.adapter.in.web.controller;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.calendar.adapter.in.web.response.GoogleOAuthStatusResponse;
import com.emme.calendar.api.command.CompleteGoogleOAuthCommand;
import com.emme.calendar.api.command.DisconnectGoogleOAuthCommand;
import com.emme.calendar.api.command.StartGoogleOAuthCommand;
import com.emme.calendar.api.query.GetGoogleOAuthStatusQuery;
import com.emme.calendar.api.type.GoogleOAuthPersona;
import com.emme.calendar.api.usecase.CompleteGoogleOAuthUseCase;
import com.emme.calendar.api.usecase.DisconnectGoogleOAuthUseCase;
import com.emme.calendar.api.usecase.GetGoogleOAuthStatusUseCase;
import com.emme.calendar.api.usecase.StartGoogleOAuthUseCase;
import com.emme.shared.web.security.CurrentUserContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/google/oauth", version = "1.0")
@Tag(name = "Google OAuth")
public class GoogleOAuthController {

  private static final Logger log = LoggerFactory.getLogger(GoogleOAuthController.class);
  private static final String STATE_PREFIX = "oauth:state:";
  private static final Duration STATE_TTL = Duration.ofMinutes(5);

  private final StartGoogleOAuthUseCase startGoogleOAuthUseCase;
  private final CompleteGoogleOAuthUseCase completeGoogleOAuthUseCase;
  private final GetGoogleOAuthStatusUseCase getGoogleOAuthStatusUseCase;
  private final DisconnectGoogleOAuthUseCase disconnectGoogleOAuthUseCase;
  private final StringRedisTemplate redis;
  private final ObjectMapper mapper;

  public GoogleOAuthController(
      StartGoogleOAuthUseCase startGoogleOAuthUseCase,
      CompleteGoogleOAuthUseCase completeGoogleOAuthUseCase,
      GetGoogleOAuthStatusUseCase getGoogleOAuthStatusUseCase,
      DisconnectGoogleOAuthUseCase disconnectGoogleOAuthUseCase,
      StringRedisTemplate redis,
      ObjectMapper mapper) {
    this.startGoogleOAuthUseCase = startGoogleOAuthUseCase;
    this.completeGoogleOAuthUseCase = completeGoogleOAuthUseCase;
    this.getGoogleOAuthStatusUseCase = getGoogleOAuthStatusUseCase;
    this.disconnectGoogleOAuthUseCase = disconnectGoogleOAuthUseCase;
    this.redis = redis;
    this.mapper = mapper;
  }

  /** Redirect to Google consent screen. Stores OAuth state in Redis for CSRF protection. */
  @GetMapping("/authorize")
  @PreAuthorize("@featureFlagService.isEnabled('google_workspace')")
  @Operation(summary = "Start Google OAuth flow")
  public void authorize(
      @RequestParam(defaultValue = "STAFF") GoogleOAuthPersona persona,
      HttpServletResponse response)
      throws IOException {
    String state = generateState();
    var context =
        withCurrentTenant(
            tenantId -> {
              var userId = CurrentUserContextHolder.currentSubject();
              return new OAuthStateContext(tenantId, userId, persona.name());
            });
    storeState(state, context);
    String url = startGoogleOAuthUseCase.start(new StartGoogleOAuthCommand(persona, state)).value();
    response.sendRedirect(url);
  }

  /**
   * Google redirects here after user consents.
   *
   * <p>Validates the OAuth state against Redis (CSRF protection and context recovery). Uses the
   * Keycloak session cookie for authentication; the path is in the permitAll list so that expired
   * JWT tokens don't block the session-based flow.
   *
   * <p>As a fallback when the session is lost, the state carries the user/tenant/persona from the
   * original authorize request.
   */
  @GetMapping("/callback")
  @Operation(summary = "OAuth callback from Google")
  public void callback(
      @RequestParam String code,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String error,
      HttpServletResponse response)
      throws IOException {
    if (error != null) {
      response.sendRedirect("/#/settings?google=denied");
      return;
    }
    if (state == null || state.isBlank()) {
      response.sendRedirect("/#/settings?google=error&reason=missing_state");
      return;
    }
    var savedContext = loadAndDeleteState(state);
    if (savedContext == null) {
      log.warn("OAuth callback with invalid/expired state: {}", state);
      response.sendRedirect("/#/settings?google=error&reason=invalid_state");
      return;
    }
    GoogleOAuthPersona persona = GoogleOAuthPersona.valueOf(savedContext.persona());
    try {
      UUID tenantId = savedContext.tenantId();
      String userId = savedContext.userId();
      completeGoogleOAuthUseCase.complete(
          new CompleteGoogleOAuthCommand(tenantId, userId, persona, code));
      response.sendRedirect("/#/settings?google=connected");
    } catch (Exception e) {
      log.error("OAuth callback failed", e);
      response.sendRedirect("/#/settings?google=error");
    }
  }

  /** Check connection status. */
  @GetMapping("/status")
  @PreAuthorize("@featureFlagService.isEnabled('google_workspace')")
  @Operation(summary = "Check Google OAuth connection status")
  public ResponseEntity<GoogleOAuthStatusResponse> status(
      @RequestParam(defaultValue = "STAFF") GoogleOAuthPersona persona) {
    return withCurrentTenant(
        tenantId -> {
          String userId = CurrentUserContextHolder.currentSubject();
          var status =
              getGoogleOAuthStatusUseCase.get(
                  new GetGoogleOAuthStatusQuery(tenantId, userId, persona));
          return ResponseEntity.ok(new GoogleOAuthStatusResponse(status.connected()));
        });
  }

  /** Disconnect (revoke tokens, delete from DB). */
  @DeleteMapping("/disconnect")
  @PreAuthorize("@featureFlagService.isEnabled('google_workspace')")
  @Operation(summary = "Disconnect Google Workspace")
  public ResponseEntity<Void> disconnect(
      @RequestParam(defaultValue = "STAFF") GoogleOAuthPersona persona) {
    return withCurrentTenant(
        tenantId -> {
          String userId = CurrentUserContextHolder.currentSubject();
          disconnectGoogleOAuthUseCase.disconnect(
              new DisconnectGoogleOAuthCommand(tenantId, userId, persona));
          return ResponseEntity.noContent().build();
        });
  }

  private String generateState() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private void storeState(String state, OAuthStateContext context) {
    try {
      String json = mapper.writeValueAsString(context);
      redis.opsForValue().set(STATE_PREFIX + state, json, STATE_TTL);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize OAuth state", e);
    }
  }

  private OAuthStateContext loadAndDeleteState(String state) {
    String key = STATE_PREFIX + state;
    String json = redis.opsForValue().getAndDelete(key);
    if (json == null) {
      return null;
    }
    try {
      return mapper.readValue(json, OAuthStateContext.class);
    } catch (JsonProcessingException e) {
      log.warn("Failed to deserialize OAuth state", e);
      return null;
    }
  }

  /** Stored in Redis with 5-min TTL. Carries context across the OAuth redirect chain. */
  private record OAuthStateContext(UUID tenantId, String userId, String persona) {}
}
