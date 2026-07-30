package com.emme.identity.api;

import java.util.List;
import java.util.UUID;

public record UserInfo(
    UUID userId,
    String email,
    String displayName,
    List<MembershipInfo> memberships,
    Object profile) {}
