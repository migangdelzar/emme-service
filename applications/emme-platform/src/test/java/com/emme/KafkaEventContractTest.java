package com.emme;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.studio.api.event.AppointmentCancelledEvent;
import com.emme.studio.api.event.AppointmentCreatedEvent;
import com.emme.studio.api.event.AppointmentRescheduledEvent;
import com.emme.tenancy.api.event.TenantCreated;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.events.Externalized;

class KafkaEventContractTest {

  @Test
  void externalizedEventsDeclareStableTopicAndTenantPartitionKey() {
    assertThat(externalizedTarget(TenantCreated.class))
        .isEqualTo("emme.tenancy.tenant-created::#{#this.tenantId()}");
    assertThat(externalizedTarget(AppointmentCreatedEvent.class))
        .isEqualTo("emme.studio.appointment-created::#{#this.tenantId()}");
    assertThat(externalizedTarget(AppointmentCancelledEvent.class))
        .isEqualTo("emme.studio.appointment-cancelled::#{#this.tenantId()}");
    assertThat(externalizedTarget(AppointmentRescheduledEvent.class))
        .isEqualTo("emme.studio.appointment-rescheduled::#{#this.tenantId()}");
  }

  @Test
  void eventContractsRemainImmutableRecords() {
    assertThat(
            Stream.of(
                TenantCreated.class,
                AppointmentCreatedEvent.class,
                AppointmentCancelledEvent.class,
                AppointmentRescheduledEvent.class))
        .allMatch(Class::isRecord);
  }

  private static String externalizedTarget(Class<?> eventType) {
    return eventType.getAnnotation(Externalized.class).value();
  }
}
