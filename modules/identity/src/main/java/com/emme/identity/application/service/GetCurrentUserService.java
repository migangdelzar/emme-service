package com.emme.identity.application.service;

import com.emme.identity.api.query.GetCurrentUserMembershipsQuery;
import com.emme.identity.api.query.GetCurrentUserQuery;
import com.emme.identity.api.result.CurrentUserDetails;
import com.emme.identity.api.result.CurrentUserMembershipDetails;
import com.emme.identity.api.result.MembershipDetails;
import com.emme.identity.api.usecase.GetCurrentUserMembershipsUseCase;
import com.emme.identity.api.usecase.GetCurrentUserUseCase;
import com.emme.identity.api.usecase.GetUserPermissionsUseCase;
import com.emme.salon.api.result.BusinessProfileSummary;
import com.emme.salon.api.usecase.GetBusinessProfileUseCase;
import com.emme.tenancy.api.query.GetTenantQuery;
import com.emme.tenancy.api.result.TenantDetails;
import com.emme.tenancy.api.usecase.GetTenantUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Application service for the current-user read workflow. */
@Service
public class GetCurrentUserService implements GetCurrentUserUseCase {

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
  public CurrentUserDetails get(GetCurrentUserQuery query) {
    List<MembershipDetails> membershipResults =
        memberships.getMemberships(new GetCurrentUserMembershipsQuery(query.userId()));
    List<CurrentUserMembershipDetails> membershipDetails =
        membershipResults.stream()
            .map(membership -> toMembershipDetails(query.userId(), membership))
            .toList();

    UUID selectedTenantId = selectedTenantId(membershipResults, query.selectedTenantId());
    BusinessProfileSummary profile = null;
    if (selectedTenantId != null) {
      try {
        profile = businessProfiles.getBusinessProfile(selectedTenantId).orElse(null);
      } catch (Exception ignored) {
        // Business profile not available for this tenant
      }
    }

    return new CurrentUserDetails(
        query.userId(), query.email(), query.displayName(), membershipDetails, profile);
  }

  private CurrentUserMembershipDetails toMembershipDetails(
      String userId, MembershipDetails membership) {
    TenantDetails tenant =
        tenants
            .get(new GetTenantQuery(membership.tenantId()))
            .orElseThrow(
                () -> new IllegalArgumentException("Tenant not found: " + membership.tenantId()));
    return new CurrentUserMembershipDetails(
        membership.tenantId(),
        tenant.slug(),
        tenant.name(),
        membership.roleCode(),
        membership.status(),
        permissions.getPermissions(userId, membership.tenantId()));
  }

  private static UUID selectedTenantId(
      List<MembershipDetails> memberships, UUID selectedTenantIdFromQuery) {
    if (selectedTenantIdFromQuery != null
        && memberships.stream()
            .anyMatch(membership -> membership.tenantId().equals(selectedTenantIdFromQuery))) {
      return selectedTenantIdFromQuery;
    }
    return memberships.size() == 1 ? memberships.getFirst().tenantId() : null;
  }
}
