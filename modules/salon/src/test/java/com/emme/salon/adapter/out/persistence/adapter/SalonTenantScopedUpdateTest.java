package com.emme.salon.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.salon.adapter.out.persistence.entity.BookingPolicyEntity;
import com.emme.salon.adapter.out.persistence.entity.BusinessProfileEntity;
import com.emme.salon.adapter.out.persistence.entity.OperatingHoursEntity;
import com.emme.salon.adapter.out.persistence.repository.SpringDataBookingPolicyRepository;
import com.emme.salon.adapter.out.persistence.repository.SpringDataBusinessProfileRepository;
import com.emme.salon.adapter.out.persistence.repository.SpringDataOperatingHoursRepository;
import com.emme.salon.domain.model.BookingPolicy;
import com.emme.salon.domain.model.BusinessProfile;
import com.emme.salon.domain.model.DayOfWeek;
import com.emme.salon.domain.model.OperatingHours;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SalonTenantScopedUpdateTest {

  @Test
  void findsTheBusinessProfileFromTheCurrentTenantSchema() {
    SpringDataBusinessProfileRepository repository = org.mockito.Mockito.mock();
    BusinessProfilePersistenceAdapter adapter = new BusinessProfilePersistenceAdapter(repository);
    BusinessProfileEntity entity =
        new BusinessProfileEntity(UUID.randomUUID(), "UTC", "en-US", "Salon");
    when(repository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(entity));

    Optional<BusinessProfile> profile = adapter.find();

    verify(repository).findFirstByOrderByCreatedAtAsc();
    assertThat(profile).isPresent();
  }

  @Test
  void findsTheBookingPolicyFromTheCurrentTenantSchema() {
    SpringDataBookingPolicyRepository repository = org.mockito.Mockito.mock();
    BookingPolicyPersistenceAdapter adapter = new BookingPolicyPersistenceAdapter(repository);
    BookingPolicyEntity entity = new BookingPolicyEntity(UUID.randomUUID(), 60, 30, 120, false);
    when(repository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(entity));

    Optional<BookingPolicy> policy = adapter.find();

    verify(repository).findFirstByOrderByCreatedAtAsc();
    assertThat(policy).isPresent();
  }

  @Test
  void findsOperatingHoursByDayFromTheCurrentTenantSchema() {
    SpringDataOperatingHoursRepository repository = org.mockito.Mockito.mock();
    OperatingHoursPersistenceAdapter adapter = new OperatingHoursPersistenceAdapter(repository);
    OperatingHoursEntity entity =
        new OperatingHoursEntity(
            UUID.randomUUID(), DayOfWeek.MON, LocalTime.of(9, 0), LocalTime.of(17, 0));
    when(repository.findByDayOfWeek(DayOfWeek.MON)).thenReturn(Optional.of(entity));

    Optional<OperatingHours> hours = adapter.findByDayOfWeek(DayOfWeek.MON);

    verify(repository).findByDayOfWeek(DayOfWeek.MON);
    assertThat(hours).isPresent();
  }

  @Test
  void updatesBookingPolicyByIdWithinTheTenantScopedConnection() {
    SpringDataBookingPolicyRepository repository = org.mockito.Mockito.mock();
    BookingPolicyPersistenceAdapter adapter = new BookingPolicyPersistenceAdapter(repository);
    UUID tenantId = UUID.randomUUID();
    UUID policyId = UUID.randomUUID();
    BookingPolicyEntity entity = new BookingPolicyEntity(tenantId, 60, 30, 120, false);
    BookingPolicy policy = BookingPolicy.reconstitute(policyId, tenantId, 15, 45, 90, true);
    when(repository.findById(policyId)).thenReturn(Optional.of(entity));
    when(repository.save(entity)).thenReturn(entity);

    BookingPolicy saved = adapter.save(policy);

    verify(repository).findById(policyId);
    assertThat(entity.getMinNoticeMinutes()).isEqualTo(15);
    assertThat(saved.getTenantId()).isEqualTo(tenantId);
  }

  @Test
  void updatesBusinessProfileByIdWithinTheTenantScopedConnection() {
    SpringDataBusinessProfileRepository repository = org.mockito.Mockito.mock();
    BusinessProfilePersistenceAdapter adapter = new BusinessProfilePersistenceAdapter(repository);
    UUID tenantId = UUID.randomUUID();
    UUID profileId = UUID.randomUUID();
    BusinessProfileEntity entity = new BusinessProfileEntity(tenantId, "UTC", "en-US", "Before");
    BusinessProfile profile =
        BusinessProfile.reconstitute(profileId, tenantId, "America/Mexico_City", "es-MX", "After");
    when(repository.findById(profileId)).thenReturn(Optional.of(entity));
    when(repository.save(entity)).thenReturn(entity);

    BusinessProfile saved = adapter.save(profile);

    verify(repository).findById(profileId);
    assertThat(entity.getTimeZone()).isEqualTo("America/Mexico_City");
    assertThat(saved.getTenantId()).isEqualTo(tenantId);
  }

  @Test
  void updatesOperatingHoursByIdWithinTheTenantScopedConnection() {
    SpringDataOperatingHoursRepository repository = org.mockito.Mockito.mock();
    OperatingHoursPersistenceAdapter adapter = new OperatingHoursPersistenceAdapter(repository);
    UUID tenantId = UUID.randomUUID();
    UUID hoursId = UUID.randomUUID();
    OperatingHoursEntity entity =
        new OperatingHoursEntity(tenantId, DayOfWeek.MON, LocalTime.of(9, 0), LocalTime.of(17, 0));
    OperatingHours operatingHours =
        OperatingHours.reconstitute(
            hoursId, tenantId, DayOfWeek.MON, LocalTime.of(10, 0), LocalTime.of(18, 0), false);
    when(repository.findById(hoursId)).thenReturn(Optional.of(entity));
    when(repository.save(entity)).thenReturn(entity);

    OperatingHours saved = adapter.save(operatingHours);

    verify(repository).findById(hoursId);
    assertThat(entity.getOpensAt()).isEqualTo(LocalTime.of(10, 0));
    assertThat(saved.getTenantId()).isEqualTo(tenantId);
  }
}
