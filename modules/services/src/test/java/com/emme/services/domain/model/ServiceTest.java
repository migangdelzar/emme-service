package com.emme.services.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServiceTest {

  @Test
  void newServiceStartsActiveWithoutPersistenceIdentity() {
    Service service = new Service(UUID.randomUUID(), "cut", "Cut", 30, BigDecimal.TEN);

    assertThat(service.getId()).isNull();
    assertThat(service.getStatus()).isEqualTo(ServiceStatus.ACTIVE);
  }

  @Test
  void retiringServiceChangesItsBusinessStatus() {
    Service service = new Service(UUID.randomUUID(), "cut", "Cut", 30, BigDecimal.TEN);

    service.retire();

    assertThat(service.getStatus()).isEqualTo(ServiceStatus.RETIRED);
  }
}
