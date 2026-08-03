package com.emme.identity.api.query;

/** Public query for active memberships belonging to a user reference. */
public record GetCurrentUserMembershipsQuery(String userReference) {}
