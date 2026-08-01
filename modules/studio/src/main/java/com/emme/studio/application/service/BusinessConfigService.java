package com.emme.studio.application.service;

import com.emme.studio.adapter.out.persistence.entity.BookingPolicyEntity;
import com.emme.studio.adapter.out.persistence.entity.BusinessProfileEntity;
import com.emme.studio.adapter.out.persistence.entity.OperatingHoursEntity;
import com.emme.studio.adapter.out.persistence.repository.SpringDataBookingPolicyRepository;
import com.emme.studio.adapter.out.persistence.repository.SpringDataBusinessProfileRepository;
import com.emme.studio.adapter.out.persistence.repository.SpringDataOperatingHoursRepository;
import com.emme.studio.domain.model.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BusinessConfigService {

  private final SpringDataBusinessProfileRepository businessProfileRepository;
  private final SpringDataOperatingHoursRepository operatingHoursRepository;
  private final SpringDataBookingPolicyRepository bookingPolicyRepository;

  public BusinessConfigService(
      SpringDataBusinessProfileRepository businessProfileRepository,
      SpringDataOperatingHoursRepository operatingHoursRepository,
      SpringDataBookingPolicyRepository bookingPolicyRepository) {
    this.businessProfileRepository = businessProfileRepository;
    this.operatingHoursRepository = operatingHoursRepository;
    this.bookingPolicyRepository = bookingPolicyRepository;
  }

  @Transactional(readOnly = true)
  public Optional<BusinessProfileEntity> getProfile(UUID tenantId) {
    return businessProfileRepository.findByTenantId(tenantId);
  }

  public BusinessProfileEntity updateProfile(
      UUID tenantId, String displayName, String timeZone, String locale) {
    BusinessProfileEntity profile =
        businessProfileRepository
            .findByTenantId(tenantId)
            .orElse(new BusinessProfileEntity(tenantId, timeZone, locale, displayName));

    profile.setDisplayName(displayName);
    profile.setTimeZone(timeZone);
    profile.setLocale(locale);

    return businessProfileRepository.save(profile);
  }

  @Transactional(readOnly = true)
  public List<OperatingHoursEntity> getOperatingHours(UUID tenantId) {
    return operatingHoursRepository.findByTenantId(tenantId);
  }

  public OperatingHoursEntity updateOperatingHours(
      UUID tenantId, DayOfWeek day, LocalTime opensAt, LocalTime closesAt, boolean active) {
    OperatingHoursEntity hours =
        operatingHoursRepository
            .findByTenantIdAndDayOfWeek(tenantId, day)
            .orElse(new OperatingHoursEntity(tenantId, day, opensAt, closesAt));

    hours.setOpensAt(opensAt);
    hours.setClosesAt(closesAt);
    hours.setActive(active);

    return operatingHoursRepository.save(hours);
  }

  @Transactional(readOnly = true)
  public Optional<BookingPolicyEntity> getBookingPolicy(UUID tenantId) {
    return bookingPolicyRepository.findByTenantId(tenantId);
  }

  public BookingPolicyEntity updateBookingPolicy(
      UUID tenantId, int minNotice, int maxAdvance, int cancelWindow, boolean allowOverlap) {
    BookingPolicyEntity policy =
        bookingPolicyRepository
            .findByTenantId(tenantId)
            .orElse(
                new BookingPolicyEntity(
                    tenantId, minNotice, maxAdvance, cancelWindow, allowOverlap));

    policy.setMinNoticeMinutes(minNotice);
    policy.setMaxAdvanceDays(maxAdvance);
    policy.setCancellationWindowMinutes(cancelWindow);
    policy.setAllowOverlap(allowOverlap);

    return bookingPolicyRepository.save(policy);
  }
}
