package com.emme.calendar.adapter.out.google.oauth;

/** Internal OAuth2 token source used by the Google Calendar outbound client. */
public interface GoogleUserTokenSource {

  String getAccessToken();

  boolean isConfigured();
}
