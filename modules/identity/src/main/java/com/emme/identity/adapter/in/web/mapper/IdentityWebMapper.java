package com.emme.identity.adapter.in.web.mapper;

import com.emme.identity.adapter.in.web.response.BusinessProfileResponse;
import com.emme.identity.adapter.in.web.response.CurrentUserResponse;
import com.emme.identity.adapter.in.web.response.MembershipResponse;
import com.emme.identity.adapter.in.web.response.TenantMembershipResponse;
import com.emme.identity.api.result.CurrentUserDetails;
import com.emme.identity.api.result.MembershipDetails;
import com.emme.identity.api.type.MembershipStatus;
import java.util.List;

/** Maps Identity application data into HTTP response models. */
public final class IdentityWebMapper {

  private IdentityWebMapper() {}

  public static MembershipResponse toMembershipResponse(MembershipDetails membership) {
    return new MembershipResponse(
        membership.id(),
        membership.tenantId(),
        membership.roleCode(),
        membership.userReference(),
        MembershipStatus.valueOf(membership.status().name()),
        membership.createdAt());
  }

  public static CurrentUserResponse toCurrentUserResponse(CurrentUserDetails user) {
    List<TenantMembershipResponse> memberships =
        user.memberships().stream()
            .map(
                membership ->
                    new TenantMembershipResponse(
                        membership.tenantId(),
                        membership.tenantSlug(),
                        membership.tenantName(),
                        membership.tenantName(),
                        membership.role(),
                        MembershipStatus.valueOf(membership.status().name()),
                        membership.permissions()))
            .toList();
    BusinessProfileResponse profile =
        user.profile() == null
            ? null
            : new BusinessProfileResponse(
                user.profile().tenantId(),
                user.profile().displayName(),
                null,
                null,
                null,
                user.profile().locale(),
                true);
    return new CurrentUserResponse(
        user.userId(), user.email(), user.displayName(), memberships, profile);
  }
}
