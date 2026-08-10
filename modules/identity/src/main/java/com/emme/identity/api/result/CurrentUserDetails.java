package com.emme.identity.api.result;

import com.emme.salon.api.result.BusinessProfileSummary;
import java.util.List;

/** Public consolidated current-user read model returned by Identity. */
public record CurrentUserDetails(
    String userId,
    String email,
    String displayName,
    List<CurrentUserMembershipDetails> memberships,
    BusinessProfileSummary profile) {

  public CurrentUserDetails {
    memberships = List.copyOf(memberships);
  }
}
