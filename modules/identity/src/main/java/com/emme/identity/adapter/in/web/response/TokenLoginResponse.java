package com.emme.identity.adapter.in.web.response;

/** HTTP response returned after staff authentication. */
public record TokenLoginResponse(
    String accessToken, String refreshToken, CurrentUserResponse user) {}
