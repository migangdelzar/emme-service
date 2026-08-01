package com.emme.identity.api.result;

import java.util.List;
import java.util.UUID;

/** Public user read model returned by Identity use cases. */
public record UserInfo(
    UUID userId,
    String email,
    String displayName,
    List<MembershipInfo> memberships,
    Object profile) {}
