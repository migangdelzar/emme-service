package com.emme.identity.application.service;

import com.emme.identity.api.query.GetCurrentUserMembershipsQuery;
import com.emme.identity.api.query.GetCurrentUserQuery;
import com.emme.identity.api.result.BusinessProfileSummary;
import com.emme.identity.api.result.CurrentUserInfo;
import com.emme.identity.api.result.CurrentUserMembershipInfo;
import com.emme.identity.api.result.MembershipInfo;
import com.emme.identity.api.usecase.GetCurrentUserMembershipsUseCase;
import com.emme.identity.api.usecase.GetCurrentUserUseCase;
import com.emme.identity.api.usecase.GetUserPermissionsUseCase;
import com.emme.studio.api.result.BusinessProfileInfo;
import com.emme.studio.api.usecase.GetBusinessProfileUseCase;
import com.emme.tenancy.api.query.GetTenantQuery;
import com.emme.tenancy.api.result.TenantInfo;
import com.emme.tenancy.api.usecase.GetTenantUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Application service for the current-user read workflow. */
@Service
public final class GetCurrentUserService implements GetCurrentUserUseCase {

  private final GetCurrentUserMembershipsUseCase memberships;
  private final GetUserPermissionsUseCase permissions;
  private final GetTenantUseCase tenants;
  private final GetBusinessProfileUseCase businessProfiles;

  public GetCurrentUserService(
      GetCurrentUserMembershipsUseCase memberships,
      GetUserPermissionsUseCase permissions,
      GetTenantUseCase tenants,
      GetBusinessProfileUseCase businessProfiles) {
    this.memberships = memberships;
    this.permissions = permissions;
    this.tenants = tenants;
    this.businessProfiles = businessProfiles;
  }

  @Override
  public CurrentUserInfo get(GetCurrentUserQuery query) {
    List<MembershipInfo> membershipResults =
        memberships.getMemberships(new GetCurrentUserMembershipsQuery(query.userId()));
    List<CurrentUserMembershipInfo> membershipViews =
        membershipResults.stream()
            .map(membership -> toMembershipView(query.userId(), membership))
            .toList();

    UUID selectedTenantId = selectedTenantId(membershipResults, query.selectedTenantId());
    BusinessProfileSummary profile =
        selectedTenantId == null
            ? null
            : businessProfiles
                .getBusinessProfile(selectedTenantId)
                .map(GetCurrentUserService::toProfileSummary)
                .orElse(null);

    return new CurrentUserInfo(
        query.userId(), query.email(), query.displayName(), membershipViews, profile);
  }

  private CurrentUserMembershipInfo toMembershipView(String userId, MembershipInfo membership) {
    TenantInfo tenant =
        tenants
            .get(new GetTenantQuery(membership.tenantId()))
            .orElseThrow(
                () -> new IllegalArgumentException("Tenant not found: " + membership.tenantId()));
    return new CurrentUserMembershipInfo(
        membership.tenantId(),
        tenant.slug(),
        tenant.name(),
        membership.roleCode(),
        membership.status(),
        permissions.getPermissions(userId, membership.tenantId()));
  }

  private static UUID selectedTenantId(
      List<MembershipInfo> memberships, UUID selectedTenantIdFromQuery) {
    if (selectedTenantIdFromQuery != null
        && memberships.stream()
            .anyMatch(membership -> membership.tenantId().equals(selectedTenantIdFromQuery))) {
      return selectedTenantIdFromQuery;
    }
    return memberships.size() == 1 ? memberships.getFirst().tenantId() : null;
  }

  private static BusinessProfileSummary toProfileSummary(BusinessProfileInfo profile) {
    return new BusinessProfileSummary(profile.tenantId(), profile.displayName(), profile.locale());
  }
}
