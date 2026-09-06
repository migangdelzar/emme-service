package com.emme.subscriptions;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.subscriptions.adapter.in.web.response.SubscriptionResponse;
import com.emme.subscriptions.api.result.SubscriptionDetails;
import com.emme.subscriptions.api.type.SubscriptionStatus;
import org.junit.jupiter.api.Test;

class SubscriptionStatusConventionTest {

  @Test
  void subscriptionStatusUsesAnApiOwnedEnumAcrossPublicBoundaries() {
    assertThat(SubscriptionDetails.class.getRecordComponents()[3].getType())
        .isEqualTo(SubscriptionStatus.class);
    assertThat(SubscriptionResponse.class.getRecordComponents()[3].getType())
        .isEqualTo(SubscriptionStatus.class);
  }
}
