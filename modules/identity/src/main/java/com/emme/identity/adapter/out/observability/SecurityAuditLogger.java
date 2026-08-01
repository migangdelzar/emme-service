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

  private static final Logger log = LoggerFactory.getLogger(SecurityAuditLogger.class);

  @EventListener
  public void onAuthSuccess(AuthenticationSuccessEvent event) {
    var auth = event.getAuthentication();
    String principal = auth.getName();
    String authorities = auth.getAuthorities().toString();
    String ip = getClientIp();
    log.info("AUTH_SUCCESS | principal={} | authorities={} | ip={}", principal, authorities, ip);
  }

  @EventListener
  public void onAuthFailure(AuthenticationFailureBadCredentialsEvent event) {
    String principal = event.getAuthentication().getName();
    String reason = event.getException().getMessage();
    String ip = getClientIp();
    log.warn("AUTH_FAILURE | principal={} | reason={} | ip={}", principal, reason, ip);
  }

  @EventListener
  public void onAccessDenied(AuthorizationDeniedEvent<?> event) {
    var auth = event.getAuthentication().get();
    String principal = auth != null ? auth.getName() : "unknown";
    String decision = event.getAuthorizationResult().toString();
    String uri = getRequestUri();
    log.warn("ACCESS_DENIED | principal={} | decision={} | uri={}", principal, decision, uri);
  }

  @EventListener
  public void onLogout(LogoutSuccessEvent event) {
    String principal = event.getAuthentication().getName();
    log.info("LOGOUT | principal={}", principal);
  }

  private String getClientIp() {
    try {
      var attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
      HttpServletRequest request = attributes.getRequest();
      String forwarded = request.getHeader("X-Forwarded-For");
      if (forwarded != null && !forwarded.isBlank()) {
        return forwarded.split(",")[0].trim();
      }
      return request.getRemoteAddr();
    } catch (IllegalStateException e) {
      return "unknown";
    }
  }

  private String getRequestUri() {
    try {
      var attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
      HttpServletRequest request = attributes.getRequest();
      return request.getRequestURI();
    } catch (IllegalStateException e) {
      return "unknown";
    }
  }
}
