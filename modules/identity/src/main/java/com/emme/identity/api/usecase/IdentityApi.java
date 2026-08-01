package com.emme.identity.api.usecase;

import com.emme.identity.api.result.MembershipInfo;
import com.emme.identity.api.result.UserInfo;
import java.util.List;
import java.util.UUID;

/**
 * Public use-case contract for identity operations.
 *
 * <p>The legacy {@code IdentityApi} name is retained for compatibility while the contract is
 * grouped under {@code api/usecase}.
 */
public interface IdentityApi {
  UserInfo getCurrentUser();

  List<MembershipInfo> getUserMemberships(UUID userId);
}
