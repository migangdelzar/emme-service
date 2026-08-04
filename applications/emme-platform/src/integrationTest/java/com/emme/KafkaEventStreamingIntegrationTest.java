package com.emme;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.studio.api.event.AppointmentCancelled;
import com.emme.studio.api.event.AppointmentCreated;
import com.emme.studio.api.event.AppointmentRescheduled;
import com.emme.tenancy.api.event.TenantCreated;
import com.emme.testing.integration.annotation.KafkaIntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.kafka.KafkaContainer;

@SpringBootTest(classes = EmmeApplication.class)
@KafkaIntegrationTest
@Import(KafkaEventStreamingIntegrationTest.OAuthClientTestConfiguration.class)
@TestPropertySource(
    properties = {
      "spring.modulith.events.externalization.enabled=true",
      "app.keycloak.provisioning.enabled=false",
      "app.google.oauth.encryption-key=12345678901234567890123456789012"
    })
@Execution(ExecutionMode.SAME_THREAD)
class KafkaEventStreamingIntegrationTest {

  @Autowired private ApplicationEventPublisher events;

  @Autowired private KafkaContainer kafka;

  @Autowired private PlatformTransactionManager transactionManager;

  @TestConfiguration(proxyBeanMethods = false)
  static class OAuthClientTestConfiguration {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
      return new InMemoryClientRegistrationRepository(
          ClientRegistration.withRegistrationId("keycloak")
              .clientId("emme-test")
              .clientSecret("emme-test-secret")
              .authorizationUri("http://localhost:8080/oauth2/authorize")
              .tokenUri("http://localhost:8080/oauth2/token")
              .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
              .scope("openid")
              .authorizationGrantType(
                  org.springframework.security.oauth2.core.AuthorizationGrantType
                      .AUTHORIZATION_CODE)
              .userNameAttributeName("sub")
              .userInfoUri("http://localhost:8080/userinfo")
              .clientName("Keycloak")
              .build());
    }
  }

  @Test
  void externalizedApplicationEventIsPublishedToItsStableKafkaTopic() {
    String tenantId = UUID.randomUUID().toString();
    String appointmentId = UUID.randomUUID().toString();
    new TransactionTemplate(transactionManager)
        .execute(
            ignored -> {
              events.publishEvent(
                  new AppointmentCancelled(
                      UUID.randomUUID(),
                      UUID.fromString(tenantId),
                      UUID.fromString(appointmentId),
                      java.time.Instant.now()));
              return null;
            });

    try (Consumer<String, String> consumer = consumer(kafka.getBootstrapServers())) {
      consumer.subscribe(java.util.List.of("emme.studio.appointment-cancelled"));
      ConsumerRecord<String, String> record = null;
      long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
      while (record == null && System.nanoTime() < deadline) {
        for (ConsumerRecord<String, String> candidate : consumer.poll(Duration.ofMillis(500))) {
          if (candidate.topic().equals("emme.studio.appointment-cancelled")) {
            record = candidate;
            break;
          }
        }
      }

      assertThat(record).as("Kafka event was not published").isNotNull();
      assertThat(record.key()).isEqualTo(tenantId);
      assertThat(record.value()).contains(appointmentId);
    }
  }

  @Test
  void everyApprovedExternalizedEventIsPublishedWithItsCatalogRouting() {
    UUID tenantId = UUID.randomUUID();
    UUID appointmentId = UUID.randomUUID();
    Instant eventTime = Instant.now();
    List<EventExpectation> expectations =
        List.of(
            new EventExpectation(
                new TenantCreated(
                    UUID.randomUUID(),
                    tenantId,
                    "streaming-salon",
                    "Streaming Salon",
                    "owner@test"),
                "emme.tenancy.tenant-created",
                tenantId.toString(),
                "streaming-salon"),
            new EventExpectation(
                new AppointmentCreated(
                    UUID.randomUUID(),
                    tenantId,
                    appointmentId,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    eventTime,
                    eventTime.plusSeconds(3_600),
                    eventTime),
                "emme.studio.appointment-created",
                tenantId.toString(),
                appointmentId.toString()),
            new EventExpectation(
                new AppointmentRescheduled(
                    UUID.randomUUID(),
                    tenantId,
                    appointmentId,
                    eventTime,
                    eventTime.plusSeconds(3_600),
                    eventTime.plusSeconds(7_200),
                    eventTime.plusSeconds(10_800),
                    eventTime),
                "emme.studio.appointment-rescheduled",
                tenantId.toString(),
                appointmentId.toString()),
            new EventExpectation(
                new AppointmentCancelled(UUID.randomUUID(), tenantId, appointmentId, eventTime),
                "emme.studio.appointment-cancelled",
                tenantId.toString(),
                appointmentId.toString()));

    new TransactionTemplate(transactionManager)
        .execute(
            ignored -> {
              expectations.forEach(expectation -> events.publishEvent(expectation.event()));
              return null;
            });

    try (Consumer<String, String> consumer = consumer(kafka.getBootstrapServers())) {
      consumer.subscribe(expectations.stream().map(EventExpectation::topic).toList());
      java.util.Map<String, ConsumerRecord<String, String>> records = new java.util.HashMap<>();
      long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
      while (records.size() < expectations.size() && System.nanoTime() < deadline) {
        for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
          records.putIfAbsent(record.topic(), record);
        }
      }

      for (EventExpectation expectation : expectations) {
        ConsumerRecord<String, String> record = records.get(expectation.topic());
        assertThat(record).as("Kafka event was not published: " + expectation.topic()).isNotNull();
        assertThat(record.key()).isEqualTo(expectation.key());
        assertThat(record.value()).contains(expectation.payloadMarker());
      }
    }
  }

  private record EventExpectation(Object event, String topic, String key, String payloadMarker) {}

  private static Consumer<String, String> consumer(String bootstrapServers) {
    var properties = new java.util.HashMap<String, Object>();
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, "emme-kafka-streaming-test");
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    return new DefaultKafkaConsumerFactory<String, String>(properties).createConsumer();
  }
}
