package com.emme.calendar.api.type;

/** Pluggable OAuth2 token source for Google Calendar API calls. */
public interface TokenSource {

  String getAccessToken();

  boolean isConfigured();
}
