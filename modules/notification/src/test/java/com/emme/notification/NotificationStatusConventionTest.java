package com.emme.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.notification.adapter.in.web.response.NotificationResponse;
import com.emme.notification.api.type.NotificationStatus;
import org.junit.jupiter.api.Test;

class NotificationStatusConventionTest {
  @Test
  void notificationResponseUsesThePublicStatusEnum() {
    assertThat(NotificationResponse.class.getRecordComponents()[4].getType())
        .isEqualTo(NotificationStatus.class);
  }
}
