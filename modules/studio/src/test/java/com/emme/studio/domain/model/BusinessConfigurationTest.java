package com.emme.studio.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BusinessConfigurationTest {

  @Test
  void operatingHoursRejectAnInvertedInterval() {
    assertThatThrownBy(
            () ->
                new OperatingHours(
                    UUID.randomUUID(), DayOfWeek.MON, LocalTime.of(18, 0), LocalTime.of(9, 0)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("opensAt must be before closesAt");
  }

  @Test
  void bookingPolicyUpdatesItsValuesAsOneBusinessConcept() {
    BookingPolicy policy = new BookingPolicy(UUID.randomUUID(), 60, 30, 120, false);

    policy.update(30, 45, 90, true);

    assertThat(policy.getMinNoticeMinutes()).isEqualTo(30);
    assertThat(policy.getMaxAdvanceDays()).isEqualTo(45);
    assertThat(policy.getCancellationWindowMinutes()).isEqualTo(90);
    assertThat(policy.isAllowOverlap()).isTrue();
  }
}
