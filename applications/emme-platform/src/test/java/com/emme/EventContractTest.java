package com.emme;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.appointments.api.event.AppointmentCancelled;
import com.emme.appointments.api.event.AppointmentCreated;
import com.emme.appointments.api.event.AppointmentRescheduled;
import com.emme.assistant.api.event.LearningCandidateEvaluationRequested;
import com.emme.calendar.api.event.CalendarSyncRequested;
import com.emme.notification.api.event.NotificationDelivered;
import com.emme.tenancy.api.event.TenantActivated;
import com.emme.tenancy.api.event.TenantCreated;
import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.events.Externalized;

class EventContractTest {

  @Test
  void currentEventsRemainInternalUntilAnExternalConsumerIsApproved() {
    assertThat(
            List.of(
                TenantCreated.class,
                TenantActivated.class,
                AppointmentCreated.class,
                AppointmentCancelled.class,
                AppointmentRescheduled.class,
                LearningCandidateEvaluationRequested.class))
        .allMatch(eventType -> !eventType.isAnnotationPresent(Externalized.class));
  }

  @Test
  void eventContractsRemainImmutableRecords() {
    assertThat(
            Stream.of(
                TenantActivated.class,
                TenantCreated.class,
                AppointmentCreated.class,
                AppointmentCancelled.class,
                AppointmentRescheduled.class,
                LearningCandidateEvaluationRequested.class))
        .allMatch(Class::isRecord);
  }

  @Test
  void currentEventsExposeStableEventIdentifiers() {
    assertThat(TenantActivated.class.getRecordComponents())
        .anyMatch(
            component ->
                component.getName().equals("eventId") && component.getType().equals(UUID.class));
    assertThat(TenantCreated.class.getRecordComponents())
        .anyMatch(
            component ->
                component.getName().equals("eventId") && component.getType().equals(UUID.class));
    assertThat(AppointmentCreated.class.getRecordComponents())
        .anyMatch(
            component ->
                component.getName().equals("eventId") && component.getType().equals(UUID.class));
  }

  @Test
  void currentPublicFactsRemainInternal() {
    List<Class<?>> allApiEvents =
        List.of(
            CalendarSyncRequested.class,
            NotificationDelivered.class,
            AppointmentCreated.class,
            AppointmentCancelled.class,
            AppointmentRescheduled.class,
            LearningCandidateEvaluationRequested.class,
            TenantActivated.class,
            TenantCreated.class);

    assertThat(allApiEvents).allMatch(type -> !type.isAnnotationPresent(Externalized.class));
  }

  @Test
  void eventPayloadsDoNotExposeFrameworkOrPersistenceTypes() {
    assertThat(
            List.of(
                    TenantActivated.class,
                    TenantCreated.class,
                    AppointmentCreated.class,
                    AppointmentCancelled.class,
                    AppointmentRescheduled.class,
                    LearningCandidateEvaluationRequested.class)
                .stream()
                .flatMap(type -> Stream.of(type.getRecordComponents()))
                .map(RecordComponent::getType))
        .noneMatch(
            type ->
                type.getName().startsWith("org.springframework.")
                    || type.getName().startsWith("jakarta.persistence.")
                    || type.getName().startsWith("org.apache.kafka."));

    assertThat(AppointmentCreated.class.getRecordComponents())
        .anyMatch(component -> component.getType().equals(Instant.class));
  }

  @Test
  void applicationDoesNotSelectRabbitOrAmqpTransport() throws IOException {
    Path repository = repositoryRoot();
    List<Path> sourceRoots = sourceRoots(repository);

    List<String> forbiddenTokens = List.of("spring-rabbit", "spring-amqp", "rabbitmq", "amqp");
    for (Path sourceRoot : sourceRoots) {
      if (!Files.exists(sourceRoot)) {
        continue;
      }
      try (var paths = Files.walk(sourceRoot)) {
        for (Path path :
            paths.filter(Files::isRegularFile).filter(EventContractTest::isTextFile).toList()) {
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

  private static List<Path> sourceRoots(Path repository) throws IOException {
    List<Path> roots =
        new ArrayList<>(
            List.of(
                repository.resolve("gradle/libs.versions.toml"),
                repository.resolve("build.gradle.kts"),
                repository.resolve("settings.gradle.kts"),
                repository.resolve("build-logic/src/main"),
                repository.resolve("infra")));
    for (String projectGroup : List.of("applications", "modules", "libraries", "database")) {
      Path groupRoot = repository.resolve(projectGroup);
      if (!Files.isDirectory(groupRoot)) {
        continue;
      }
      try (Stream<Path> projects = Files.list(groupRoot)) {
        projects
            .filter(Files::isDirectory)
            .forEach(
                project -> {
                  roots.add(project.resolve("build.gradle.kts"));
                  roots.add(project.resolve("src/main"));
                });
      }
    }
    return roots;
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
}
