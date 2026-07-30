package com.emme.calendar.api;

/**
 * Pluggable OAuth2 token source for Google Calendar API calls. Implementations provide Bearer
 * tokens from different auth methods (service account, user OAuth, etc.).
 */
public interface TokenSource {
  /** Returns a fresh Bearer token. */
  String getAccessToken();

  /** Whether this token source is configured and ready. */
  boolean isConfigured();
}
