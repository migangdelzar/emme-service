package com.emme.identity.service;

import com.emme.identity.adapter.out.persistence.entity.Membership;
import com.emme.identity.adapter.out.persistence.repository.MembershipRepository;
import com.emme.identity.api.result.MembershipInfo;
import com.emme.identity.api.result.UserInfo;
import com.emme.identity.api.usecase.IdentityApi;
import com.emme.tenancy.api.usecase.TenantApi;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class IdentityApiImpl implements IdentityApi {

  private final MembershipRepository membershipRepository;
  private final TenantApi tenantApi;

  IdentityApiImpl(MembershipRepository membershipRepository, TenantApi tenantApi) {
    this.membershipRepository = membershipRepository;
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
    return membershipRepository.findAll().stream()
        .filter(m -> userReference.equals(m.getUserReference()))
        .map(this::toMembershipInfo)
        .toList();
  }

  private MembershipInfo toMembershipInfo(Membership m) {
    String tenantName;
    try {
      tenantName = tenantApi.getTenantInfo(m.getTenantId()).name();
    } catch (IllegalArgumentException e) {
      tenantName = "Unknown";
    }
    return new MembershipInfo(
        m.getId(),
        m.getTenantId(),
        tenantName,
        m.getRole() != null ? m.getRole().getCode() : "UNKNOWN",
        m.getStatus().name());
  }
}
