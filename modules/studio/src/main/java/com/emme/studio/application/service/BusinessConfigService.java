package com.emme.studio.application.service;

import com.emme.studio.application.port.out.BookingPolicyRepository;
import com.emme.studio.application.port.out.BusinessProfileRepository;
import com.emme.studio.application.port.out.OperatingHoursRepository;
import com.emme.studio.domain.model.BookingPolicy;
import com.emme.studio.domain.model.BusinessProfile;
import com.emme.studio.domain.model.DayOfWeek;
import com.emme.studio.domain.model.OperatingHours;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BusinessConfigService {

  private final BusinessProfileRepository businessProfileRepository;
  private final OperatingHoursRepository operatingHoursRepository;
  private final BookingPolicyRepository bookingPolicyRepository;

  public BusinessConfigService(
      BusinessProfileRepository businessProfileRepository,
      OperatingHoursRepository operatingHoursRepository,
      BookingPolicyRepository bookingPolicyRepository) {
    this.businessProfileRepository = businessProfileRepository;
    this.operatingHoursRepository = operatingHoursRepository;
    this.bookingPolicyRepository = bookingPolicyRepository;
  }

  @Transactional(readOnly = true)
  public Optional<BusinessProfile> getProfile(UUID tenantId) {
    return businessProfileRepository.findByTenantId(tenantId);
  }

  public BusinessProfile updateProfile(
      UUID tenantId, String displayName, String timeZone, String locale) {
    BusinessProfile profile =
        businessProfileRepository
            .findByTenantId(tenantId)
            .orElse(new BusinessProfile(tenantId, timeZone, locale, displayName));
    profile.update(timeZone, locale, displayName);
    return businessProfileRepository.save(profile);
  }

  @Transactional(readOnly = true)
  public List<OperatingHours> getOperatingHours(UUID tenantId) {
    return operatingHoursRepository.findByTenantId(tenantId);
  }

  public OperatingHours updateOperatingHours(
      UUID tenantId, DayOfWeek day, LocalTime opensAt, LocalTime closesAt, boolean active) {
    OperatingHours hours =
        operatingHoursRepository
            .findByTenantIdAndDayOfWeek(tenantId, day)
            .orElse(new OperatingHours(tenantId, day, opensAt, closesAt));
    hours.update(opensAt, closesAt, active);
    return operatingHoursRepository.save(hours);
  }

  @Transactional(readOnly = true)
  public Optional<BookingPolicy> getBookingPolicy(UUID tenantId) {
    return bookingPolicyRepository.findByTenantId(tenantId);
  }

  public BookingPolicy updateBookingPolicy(
      UUID tenantId, int minNotice, int maxAdvance, int cancelWindow, boolean allowOverlap) {
    BookingPolicy policy =
        bookingPolicyRepository
            .findByTenantId(tenantId)
            .orElse(new BookingPolicy(tenantId, minNotice, maxAdvance, cancelWindow, allowOverlap));
    policy.update(minNotice, maxAdvance, cancelWindow, allowOverlap);
    return bookingPolicyRepository.save(policy);
  }
}
