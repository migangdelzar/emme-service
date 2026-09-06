package com.emme.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.adapter.in.web.response.MembershipResponse;
import com.emme.identity.adapter.in.web.response.TenantMembershipResponse;
import com.emme.identity.api.result.CurrentUserMembershipDetails;
import com.emme.identity.api.result.MembershipDetails;
import com.emme.identity.domain.model.MembershipStatus;
import org.junit.jupiter.api.Test;

class MembershipStatusConventionTest {

  @Test
  void membershipStatusUsesTheDomainEnumAcrossPublicBoundaries() {
    assertThat(MembershipDetails.class.getRecordComponents()[5].getType())
        .isEqualTo(MembershipStatus.class);
    assertThat(CurrentUserMembershipDetails.class.getRecordComponents()[4].getType())
        .isEqualTo(MembershipStatus.class);
    assertThat(MembershipResponse.class.getRecordComponents()[4].getType())
        .isEqualTo(MembershipStatus.class);
    assertThat(TenantMembershipResponse.class.getRecordComponents()[5].getType())
        .isEqualTo(MembershipStatus.class);
  }
}
