package com.emme;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.calendar.api.event.CalendarSyncRequested;
import com.emme.notification.api.event.NotificationDelivered;
import com.emme.studio.api.event.AppointmentCancelledEvent;
import com.emme.studio.api.event.AppointmentCreatedEvent;
import com.emme.studio.api.event.AppointmentRescheduledEvent;
import com.emme.studio.api.event.DashboardEvent;
import com.emme.tenancy.api.event.TenantCreated;
import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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

  @Test
  void externalizedEventsExposeStableEventIdentifiers() {
    assertThat(TenantCreated.class.getRecordComponents())
        .anyMatch(
            component ->
                component.getName().equals("eventId") && component.getType().equals(UUID.class));
    assertThat(AppointmentCreatedEvent.class.getRecordComponents())
        .anyMatch(
            component ->
                component.getName().equals("eventId") && component.getType().equals(UUID.class));
  }

  @Test
  void onlyApprovedPublicFactsAreExternalized() {
    List<Class<?>> allApiEvents =
        List.of(
            CalendarSyncRequested.class,
            NotificationDelivered.class,
            AppointmentCreatedEvent.class,
            AppointmentCancelledEvent.class,
            AppointmentRescheduledEvent.class,
            DashboardEvent.class,
            TenantCreated.class);

    assertThat(allApiEvents.stream().filter(type -> type.isAnnotationPresent(Externalized.class)))
        .containsExactlyInAnyOrder(
            AppointmentCreatedEvent.class,
            AppointmentCancelledEvent.class,
            AppointmentRescheduledEvent.class,
            TenantCreated.class);

    assertThat(
            List.of(CalendarSyncRequested.class, NotificationDelivered.class, DashboardEvent.class))
        .allMatch(type -> !type.isAnnotationPresent(Externalized.class));
  }

  @Test
  void externalizedPayloadsDoNotExposeFrameworkOrPersistenceTypes() {
    assertThat(
            List.of(
                    TenantCreated.class,
                    AppointmentCreatedEvent.class,
                    AppointmentCancelledEvent.class,
                    AppointmentRescheduledEvent.class)
                .stream()
                .flatMap(type -> Stream.of(type.getRecordComponents()))
                .map(RecordComponent::getType))
        .noneMatch(
            type ->
                type.getName().startsWith("org.springframework.")
                    || type.getName().startsWith("jakarta.persistence.")
                    || type.getName().startsWith("org.apache.kafka."));

    assertThat(AppointmentCreatedEvent.class.getRecordComponents())
        .anyMatch(component -> component.getType().equals(Instant.class));
  }

  @Test
  void applicationDoesNotSelectRabbitOrAmqpTransport() throws IOException {
    Path repository = repositoryRoot();
    List<Path> sourceRoots =
        List.of(
            repository.resolve("gradle/libs.versions.toml"),
            repository.resolve("build.gradle.kts"),
            repository.resolve("settings.gradle.kts"),
            repository.resolve("build-logic"),
            repository.resolve("applications"),
            repository.resolve("modules"),
            repository.resolve("libraries"),
            repository.resolve("database"),
            repository.resolve("infra"));

    List<String> forbiddenTokens = List.of("spring-rabbit", "spring-amqp", "rabbitmq", "amqp");
    for (Path sourceRoot : sourceRoots) {
      if (!Files.exists(sourceRoot)) {
        continue;
      }
      try (var paths = Files.walk(sourceRoot)) {
        for (Path path :
            paths
                .filter(Files::isRegularFile)
                .filter(KafkaEventContractTest::isTextFile)
                .toList()) {
          String content = Files.readString(path).toLowerCase();
          for (String forbiddenToken : forbiddenTokens) {
            assertThat(content)
                .describedAs("Unsupported RabbitMQ/AMQP transport reference in %s", path)
                .doesNotContain(forbiddenToken);
          }
        }
      }
    }
  }

  private static Path repositoryRoot() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      if (Files.exists(current.resolve("settings.gradle.kts"))) {
        return current;
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Repository root not found");
  }

  private static boolean isTextFile(Path path) {
    String pathText = path.toString();
    if (pathText.contains("/build/")
        || pathText.contains("/.gradle/")
        || pathText.contains("/src/test/")
        || pathText.contains("/src/integrationTest/")) {
      return false;
    }
    String name = path.getFileName().toString().toLowerCase();
    return name.endsWith(".java")
        || name.endsWith(".kt")
        || name.endsWith(".kts")
        || name.endsWith(".toml")
        || name.endsWith(".yml")
        || name.endsWith(".yaml")
        || name.endsWith(".xml")
        || name.endsWith(".properties");
  }

  private static String externalizedTarget(Class<?> eventType) {
    return eventType.getAnnotation(Externalized.class).value();
  }
}
