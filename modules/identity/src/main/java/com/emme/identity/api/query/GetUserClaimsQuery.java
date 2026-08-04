package com.emme.identity.api.query;

/** Request to retrieve verified user claims for an access token. */
public record GetUserClaimsQuery(String accessToken) {}
