package com.emme.calendar.application.port.out;

/** Provider-neutral token data exchanged during a Google OAuth flow. */
public record GoogleOAuthTokens(
    String accessToken, String refreshToken, String scope, long expiresIn, String email) {}
