package com.emme.identity.api.result;

import java.util.List;
import java.util.UUID;

/** Public user read model returned by Identity use cases. */
public record UserDetails(
    UUID userId,
    String email,
    String displayName,
    List<MembershipDetails> memberships,
    Object profile) {}
