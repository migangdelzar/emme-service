package com.emme.services.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.services.adapter.out.persistence.entity.ServiceEntity;
import com.emme.services.adapter.out.persistence.repository.SpringDataServiceRepository;
import com.emme.services.domain.model.ServiceStatus;
import com.emme.testing.BaseRepositoryTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(com.emme.testing.TestSecurityConfig.class)
class ServiceRepositoryTest extends BaseRepositoryTest {

  private static final UUID TENANT_ID = UUID.randomUUID();

  @Autowired private SpringDataServiceRepository serviceRepository;
  @Autowired private EntityManagerFactory entityManagerFactory;
  @PersistenceContext private EntityManager entityManager;

  @Test
  void savesAndFindsServiceById() {
    ServiceEntity saved =
        serviceRepository.save(
            new ServiceEntity(TENANT_ID, "CUT", "Cut", 30, BigDecimal.valueOf(25)));

    assertThat(saved.getId()).isNotNull();
    assertThat(serviceRepository.findById(saved.getId()))
        .get()
        .extracting(ServiceEntity::getName)
        .isEqualTo("Cut");
  }

  @Test
  void listsActiveServicesWithOneQueryAfterPendingInsertsAreFlushed() {
    serviceRepository.save(new ServiceEntity(TENANT_ID, "CUT", "Cut", 30, BigDecimal.valueOf(25)));
    serviceRepository.save(new ServiceEntity(TENANT_ID, "GEL", "Gel", 60, BigDecimal.valueOf(40)));
    entityManager.flush();

    Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.clear();

    assertThat(serviceRepository.findByStatus(ServiceStatus.ACTIVE)).hasSize(2);
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
  }
}
