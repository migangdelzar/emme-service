package com.emme.salon.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.salon.adapter.out.persistence.entity.BookingPolicyEntity;
import com.emme.salon.adapter.out.persistence.entity.BusinessProfileEntity;
import com.emme.salon.adapter.out.persistence.entity.OperatingHoursEntity;
import com.emme.salon.adapter.out.persistence.repository.SpringDataBookingPolicyRepository;
import com.emme.salon.adapter.out.persistence.repository.SpringDataBusinessProfileRepository;
import com.emme.salon.adapter.out.persistence.repository.SpringDataOperatingHoursRepository;
import com.emme.salon.domain.model.DayOfWeek;
import com.emme.testing.BaseRepositoryTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import java.time.LocalTime;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(com.emme.testing.TestSecurityConfig.class)
class SalonRepositoryTest extends BaseRepositoryTest {

  private static final UUID TENANT_ID = UUID.randomUUID();

  @Autowired private SpringDataBusinessProfileRepository profileRepository;
  @Autowired private SpringDataBookingPolicyRepository policyRepository;
  @Autowired private SpringDataOperatingHoursRepository hoursRepository;
  @Autowired private EntityManagerFactory entityManagerFactory;
  @PersistenceContext private EntityManager entityManager;

  @Test
  void savesAndFindsTenantSingletonConfiguration() {
    BusinessProfileEntity profile =
        profileRepository.save(new BusinessProfileEntity(TENANT_ID, "UTC", "en-US", "Studio"));
    policyRepository.save(new BookingPolicyEntity(TENANT_ID, 60, 30, 120, false));

    assertThat(profileRepository.findFirstByOrderByCreatedAtAsc())
        .get()
        .extracting(BusinessProfileEntity::getDisplayName)
        .isEqualTo("Studio");
    assertThat(policyRepository.findFirstByOrderByCreatedAtAsc()).isPresent();
  }

  @Test
  void listsOperatingHoursWithOneQueryAfterPendingInsertsAreFlushed() {
    hoursRepository.save(
        new OperatingHoursEntity(
            TENANT_ID, DayOfWeek.MON, LocalTime.of(9, 0), LocalTime.of(17, 0)));
    hoursRepository.save(
        new OperatingHoursEntity(
            TENANT_ID, DayOfWeek.TUE, LocalTime.of(9, 0), LocalTime.of(17, 0)));
    entityManager.flush();

    Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.clear();

    assertThat(hoursRepository.findAll()).hasSize(2);
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
  }
}
