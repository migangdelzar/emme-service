package com.emme.identity.api.result;

import java.util.List;

/** Public consolidated current-user read model returned by Identity. */
public record CurrentUserInfo(
    String userId,
    String email,
    String displayName,
    List<CurrentUserMembershipInfo> memberships,
    BusinessProfileSummary profile) {

  public CurrentUserInfo {
    memberships = List.copyOf(memberships);
  }
}
