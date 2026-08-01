package com.emme.identity.adapter.in.web.security;

import com.emme.functional.unchecked.UFunction;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;

/** Reads the authenticated user context from Spring Security's request context. */
public final class UserContextHolder {

  private UserContextHolder() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static String currentSubject() {
    return currentUser().subject();
  }

  public static String currentEmail() {
    return currentUser().email();
  }

  public static <T> T withCurrentUser(UFunction<UserContext, T> action) {
    return action.apply(currentUser());
  }

  public static UserContext currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      throw new IllegalStateException("No authenticated user context");
    }
    return fromPrincipal(authentication.getPrincipal());
  }

  public static UserContext fromPrincipal(Object principal) {
    if (principal instanceof Jwt jwt) {
      String subject = jwt.getSubject();
      if (subject == null || subject.isBlank()) {
        subject = jwt.getClaimAsString("preferred_username");
      }
      return new UserContext(
          requireSubject(subject),
          claim(jwt, "email", ""),
          claim(jwt, "name", claim(jwt, "preferred_username", subject)),
          uuidClaim(jwt.getClaimAsString("tenant_id")));
    }

    if (principal instanceof OidcUser oidcUser) {
      String subject = oidcUser.getPreferredUsername();
      if (subject == null || subject.isBlank()) {
        subject = oidcUser.getSubject();
      }
      return new UserContext(
          requireSubject(subject),
          valueOrEmpty(oidcUser.getEmail()),
          valueOrFallback(oidcUser.getFullName(), subject),
          uuidAttribute(oidcUser.getAttribute("tenant_id")));
    }

    throw new IllegalStateException("No authenticated user context");
  }

  private static String requireSubject(String subject) {
    if (subject == null || subject.isBlank()) {
      throw new IllegalStateException("No authenticated user context");
    }
    return subject;
  }

  private static String claim(Jwt jwt, String name, String fallback) {
    String value = jwt.getClaimAsString(name);
    return valueOrFallback(value, fallback);
  }

  private static String valueOrEmpty(String value) {
    return value == null ? "" : value;
  }

  private static String valueOrFallback(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private static UUID uuidClaim(String claim) {
    return claim == null || claim.isBlank() ? null : UUID.fromString(claim);
  }

  private static UUID uuidAttribute(Object attribute) {
    return attribute instanceof String value && !value.isBlank() ? UUID.fromString(value) : null;
  }
}
