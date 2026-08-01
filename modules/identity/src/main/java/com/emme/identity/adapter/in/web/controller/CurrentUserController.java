package com.emme.identity.adapter.in.web.controller;

import com.emme.identity.adapter.in.web.mapper.IdentityWebMapper;
import com.emme.identity.adapter.in.web.response.BusinessProfileResponse;
import com.emme.identity.adapter.in.web.response.CurrentUserResponse;
import com.emme.identity.adapter.in.web.response.TenantMembershipResponse;
import com.emme.identity.adapter.in.web.security.UserContext;
import com.emme.identity.adapter.in.web.security.UserContextHolder;
import com.emme.identity.api.query.GetCurrentUserMembershipsQuery;
import com.emme.identity.api.result.MembershipInfo;
import com.emme.identity.api.usecase.GetCurrentUserMembershipsUseCase;
import com.emme.identity.api.usecase.GetUserPermissionsUseCase;
import com.emme.studio.api.usecase.SalonApi;
import com.emme.tenancy.api.query.GetTenantQuery;
import com.emme.tenancy.api.result.TenantInfo;
import com.emme.tenancy.api.usecase.GetTenantUseCase;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrentUserController {

  private final GetUserPermissionsUseCase permissions;
  private final GetCurrentUserMembershipsUseCase memberships;
  private final GetTenantUseCase getTenant;
  private final SalonApi salonApi;

  public CurrentUserController(
      GetUserPermissionsUseCase permissions,
      GetCurrentUserMembershipsUseCase memberships,
      GetTenantUseCase getTenant,
      SalonApi salonApi) {
    this.permissions = permissions;
    this.memberships = memberships;
    this.getTenant = getTenant;
    this.salonApi = salonApi;
  }

  @GetMapping("/api/me")
  public CurrentUserResponse currentUser(@AuthenticationPrincipal Object principal) {
    UserContext user = UserContextHolder.fromPrincipal(principal);

    List<MembershipInfo> memberships =
        this.memberships.getMemberships(new GetCurrentUserMembershipsQuery(user.subject()));
    List<TenantMembershipResponse> membershipResponses =
        memberships.stream().map(membership -> toResponse(user.subject(), membership)).toList();

    UUID selectedTenantId = selectedTenantId(memberships, user.tenantId());
    BusinessProfileResponse profile =
        selectedTenantId == null
            ? null
            : salonApi
                .getBusinessProfile(selectedTenantId)
                .map(IdentityWebMapper::toBusinessProfileResponse)
                .orElse(null);

    return new CurrentUserResponse(
        user.subject(), user.email(), user.displayName(), membershipResponses, profile);
  }

  private TenantMembershipResponse toResponse(String subject, MembershipInfo membership) {
    TenantInfo tenant =
        getTenant
            .get(new GetTenantQuery(membership.tenantId()))
            .orElseThrow(
                () -> new IllegalArgumentException("Tenant not found: " + membership.tenantId()));
    Set<String> permissionCodes = permissions.getPermissions(subject, membership.tenantId());
    return new TenantMembershipResponse(
        membership.tenantId(),
        tenant.slug(),
        tenant.name(),
        tenant.name(),
        membership.roleCode(),
        membership.status(),
        permissionCodes);
  }

  private static UUID selectedTenantId(List<MembershipInfo> memberships, UUID tenantIdFromClaim) {
    if (tenantIdFromClaim != null) {
      if (memberships.stream().anyMatch(m -> m.tenantId().equals(tenantIdFromClaim))) {
        return tenantIdFromClaim;
      }
    }
    return memberships.size() == 1 ? memberships.getFirst().tenantId() : null;
  }
}
