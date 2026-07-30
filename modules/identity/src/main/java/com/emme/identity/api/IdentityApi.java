package com.emme.identity.api;

import java.util.List;
import java.util.UUID;

/**
 * Public API for identity operations. Business modules depend on this interface, not on internal
 * entity classes.
 */
public interface IdentityApi {
  UserInfo getCurrentUser();

  List<MembershipInfo> getUserMemberships(UUID userId);
}
