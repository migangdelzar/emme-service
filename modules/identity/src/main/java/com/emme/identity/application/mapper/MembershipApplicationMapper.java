package com.emme.identity.application.mapper;

import com.emme.identity.api.result.MembershipDetails;
import com.emme.identity.api.type.MembershipStatus;
import com.emme.identity.domain.model.Membership;

/** Maps Identity membership domain objects to public application results. */
public final class MembershipApplicationMapper {

  private MembershipApplicationMapper() {}

  public static MembershipDetails toResult(Membership membership) {
    return new MembershipDetails(
        membership.id(),
        membership.tenantId(),
        null,
        membership.roleCode(),
        membership.userReference(),
        MembershipStatus.valueOf(membership.status().name()),
        membership.createdAt());
  }
}
