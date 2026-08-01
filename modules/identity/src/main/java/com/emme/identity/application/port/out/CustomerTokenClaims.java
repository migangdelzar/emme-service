package com.emme.identity.application.port.out;

/** Verified, transport-neutral customer claims returned by a token decoder port. */
public record CustomerTokenClaims(
    String issuer,
    String subject,
    String email,
    String name,
    String identityProvider,
    String avatarUrl) {}
