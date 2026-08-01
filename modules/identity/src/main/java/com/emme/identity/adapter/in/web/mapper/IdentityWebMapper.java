package com.emme.identity.adapter.in.web.mapper;

import com.emme.identity.adapter.in.web.response.BusinessProfileResponse;
import com.emme.identity.adapter.in.web.response.MembershipResponse;
import com.emme.identity.adapter.out.persistence.entity.Membership;
import com.emme.studio.api.result.BusinessProfileInfo;

/** Maps Identity application data into HTTP response models. */
public final class IdentityWebMapper {

  private IdentityWebMapper() {}

  public static MembershipResponse toMembershipResponse(Membership membership) {
    return new MembershipResponse(
        membership.getId(),
        membership.getTenantId(),
        membership.getRole().getCode(),
        membership.getUserReference(),
        membership.getStatus().name(),
        membership.getCreatedAt());
  }

  public static BusinessProfileResponse toBusinessProfileResponse(BusinessProfileInfo profile) {
    return new BusinessProfileResponse(
        profile.tenantId(), profile.displayName(), null, null, null, profile.locale(), true);
  }
}
