package com.emme.identity.application.service;

import com.emme.identity.api.result.MembershipInfo;
import com.emme.identity.api.result.UserInfo;
import com.emme.identity.api.usecase.IdentityApi;
import com.emme.identity.domain.model.Membership;
import com.emme.tenancy.api.usecase.TenantApi;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implements the public Identity API through application services. */
@Service
@Transactional(readOnly = true)
class IdentityApiService implements IdentityApi {

  private final MembershipService membershipService;
  private final TenantApi tenantApi;

  IdentityApiService(MembershipService membershipService, TenantApi tenantApi) {
    this.membershipService = membershipService;
    this.tenantApi = tenantApi;
  }

  @Override
  public UserInfo getCurrentUser() {
    // For now, return empty — CurrentUserController fills this from JWT
    return new UserInfo(null, "", null, Collections.emptyList(), null);
  }

  @Override
  public List<MembershipInfo> getUserMemberships(UUID userId) {
    // tenantId is stored as user_reference (string) in membership table
    // For lookup by UUID userId, we query membership by user_reference
    String userReference = userId.toString();
    return membershipService.findUserMemberships(userReference).stream()
        .map(this::toMembershipInfo)
        .toList();
  }

  private MembershipInfo toMembershipInfo(Membership m) {
    String tenantName;
    try {
      tenantName = tenantApi.getTenantInfo(m.tenantId()).name();
    } catch (IllegalArgumentException e) {
      tenantName = "Unknown";
    }
    return new MembershipInfo(m.id(), m.tenantId(), tenantName, m.roleCode(), m.status().name());
  }
}
