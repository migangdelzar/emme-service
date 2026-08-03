package com.emme.studio.application.mapper;

import com.emme.studio.api.result.BookingPolicyDetails;
import com.emme.studio.api.result.BusinessProfileDetails;
import com.emme.studio.api.result.OperatingHoursDetails;
import com.emme.studio.api.type.BusinessDay;
import com.emme.studio.domain.model.BookingPolicy;
import com.emme.studio.domain.model.BusinessProfile;
import com.emme.studio.domain.model.OperatingHours;

/** Maps business-configuration domain objects to public use-case results. */
public final class BusinessConfigurationApplicationMapper {

  private BusinessConfigurationApplicationMapper() {}

  public static BusinessProfileDetails toDetails(BusinessProfile profile) {
    return new BusinessProfileDetails(
        profile.getId(), profile.getDisplayName(), profile.getTimeZone(), profile.getLocale());
  }

  public static OperatingHoursDetails toDetails(OperatingHours hours) {
    return new OperatingHoursDetails(
        hours.getId(),
        BusinessDay.valueOf(hours.getDayOfWeek().name()),
        hours.getOpensAt(),
        hours.getClosesAt(),
        hours.isActive());
  }

  public static BookingPolicyDetails toDetails(BookingPolicy policy) {
    return new BookingPolicyDetails(
        policy.getId(),
        policy.getMinNoticeMinutes(),
        policy.getMaxAdvanceDays(),
        policy.getCancellationWindowMinutes(),
        policy.isAllowOverlap());
  }
}
