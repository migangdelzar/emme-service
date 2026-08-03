package com.emme.identity.api.result;

/** Token set returned by the Identity provider after user authentication. */
public record UserTokenResult(String accessToken, String refreshToken, String idToken) {}
