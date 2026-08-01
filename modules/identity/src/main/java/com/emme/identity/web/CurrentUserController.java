package com.emme.identity.web;

import com.emme.identity.UserContext;
import com.emme.identity.UserContextHolder;
import com.emme.identity.application.IdentityService;
import com.emme.identity.entity.Membership;
import com.emme.studio.api.result.BusinessProfileInfo;
import com.emme.studio.api.usecase.SalonApi;
import com.emme.tenancy.api.TenantApi;
import com.emme.tenancy.api.TenantInfo;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrentUserController {

  private final IdentityService identityService;
  private final TenantApi tenantApi;
  private final SalonApi salonApi;

  public CurrentUserController(
      IdentityService identityService, TenantApi tenantApi, SalonApi salonApi) {
    this.identityService = identityService;
    this.tenantApi = tenantApi;
    this.salonApi = salonApi;
  }

  @GetMapping("/api/me")
  public CurrentUserResponse currentUser(@AuthenticationPrincipal Object principal) {
    UserContext user = UserContextHolder.fromPrincipal(principal);

    List<Membership> memberships = identityService.getCurrentUserMemberships(user.subject());
    List<TenantMembershipResponse> membershipResponses =
        memberships.stream().map(membership -> toResponse(user.subject(), membership)).toList();

    UUID selectedTenantId = selectedTenantId(memberships, user.tenantId());
    BusinessProfileResponse profile =
        selectedTenantId == null
            ? null
            : salonApi
                .getBusinessProfile(selectedTenantId)
                .map(BusinessProfileResponse::from)
                .orElse(null);

    return new CurrentUserResponse(
        user.subject(), user.email(), user.displayName(), membershipResponses, profile);
  }

  private TenantMembershipResponse toResponse(String subject, Membership membership) {
    TenantInfo tenant = tenantApi.getTenantInfo(membership.getTenantId());
    Set<String> permissions =
        identityService.getPermissionsForUser(subject, membership.getTenantId());
    return new TenantMembershipResponse(
        membership.getTenantId(),
        tenant.slug(),
        tenant.name(),
        tenant.name(),
        membership.getRole().getCode(),
        membership.getStatus().name(),
        permissions);
  }

  private static UUID selectedTenantId(List<Membership> memberships, UUID tenantIdFromClaim) {
    if (tenantIdFromClaim != null) {
      if (memberships.stream().anyMatch(m -> m.getTenantId().equals(tenantIdFromClaim))) {
        return tenantIdFromClaim;
      }
    }
    return memberships.size() == 1 ? memberships.getFirst().getTenantId() : null;
  }

  public record CurrentUserResponse(
      String userId,
      String email,
      String displayName,
      List<TenantMembershipResponse> memberships,
      BusinessProfileResponse profile) {}

  public record TenantMembershipResponse(
      UUID tenantId,
      String tenantSlug,
      String tenantName,
      String displayName,
      String role,
      String status,
      Set<String> permissions) {}

  public record BusinessProfileResponse(
      UUID tenantId,
      String businessName,
      String ownerName,
      String monthlyGoal,
      String workingHours,
      String language,
      boolean notificationsEnabled) {
    static BusinessProfileResponse from(BusinessProfileInfo profile) {
      return new BusinessProfileResponse(
          profile.tenantId(), profile.displayName(), null, null, null, profile.locale(), true);
    }
  }
}
