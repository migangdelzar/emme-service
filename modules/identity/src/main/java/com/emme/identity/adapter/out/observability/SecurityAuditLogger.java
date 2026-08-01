package com.emme.identity.adapter.out.observability;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class SecurityAuditLogger {

  private static final int MAX_AUDIT_VALUE_LENGTH = 256;
  private static final Logger log = LoggerFactory.getLogger(SecurityAuditLogger.class);

  @EventListener
  public void onAuthSuccess(AuthenticationSuccessEvent event) {
    var auth = event.getAuthentication();
    String principal = safeLogValue(auth.getName());
    String authorities = safeLogValue(auth.getAuthorities().toString());
    String ip = getClientIp();
    log.info("AUTH_SUCCESS | principal={} | authorities={} | ip={}", principal, authorities, ip);
  }

  @EventListener
  public void onAuthFailure(AuthenticationFailureBadCredentialsEvent event) {
    String principal = safeLogValue(event.getAuthentication().getName());
    String reason = safeFailureReason(event.getException());
    String ip = getClientIp();
    log.warn("AUTH_FAILURE | principal={} | reason={} | ip={}", principal, reason, ip);
  }

  @EventListener
  public void onAccessDenied(AuthorizationDeniedEvent<?> event) {
    var auth = event.getAuthentication().get();
    String principal = auth != null ? safeLogValue(auth.getName()) : "unknown";
    String decision = safeLogValue(event.getAuthorizationResult().toString());
    String uri = getRequestUri();
    log.warn("ACCESS_DENIED | principal={} | decision={} | uri={}", principal, decision, uri);
  }

  @EventListener
  public void onLogout(LogoutSuccessEvent event) {
    String principal = safeLogValue(event.getAuthentication().getName());
    log.info("LOGOUT | principal={}", principal);
  }

  static String safeFailureReason(Throwable exception) {
    return exception == null ? "unknown" : exception.getClass().getSimpleName();
  }

  static String safeLogValue(String value) {
    if (value == null) {
      return "unknown";
    }

    StringBuilder sanitized = new StringBuilder(Math.min(value.length(), MAX_AUDIT_VALUE_LENGTH));
    for (int index = 0;
        index < value.length() && sanitized.length() < MAX_AUDIT_VALUE_LENGTH;
        index++) {
      char character = value.charAt(index);
      sanitized.append(Character.isISOControl(character) ? '?' : character);
    }
    return sanitized.toString();
  }

  static String clientIp(HttpServletRequest request) {
    return safeLogValue(request.getRemoteAddr());
  }

  static String requestUri(HttpServletRequest request) {
    return safeLogValue(request.getRequestURI());
  }

  private String getClientIp() {
    try {
      var attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
      HttpServletRequest request = attributes.getRequest();
      return clientIp(request);
    } catch (IllegalStateException e) {
      return "unknown";
    }
  }

  private String getRequestUri() {
    try {
      var attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
      HttpServletRequest request = attributes.getRequest();
      return requestUri(request);
    } catch (IllegalStateException e) {
      return "unknown";
    }
  }
}
