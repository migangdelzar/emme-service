package com.emme.calendar;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CalendarPackageConventionTest {

  private static final Path SOURCE_ROOT =
      sourcePath("modules/calendar/src/main/java/com/emme/calendar");

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.emme.calendar");

  @Test
  void calendarProductionTypesDoNotRemainInLegacyPackages() {
    Set<String> legacyPrefixes =
        Set.of(
            "com.emme.calendar.application",
            "com.emme.calendar.entity",
            "com.emme.calendar.event",
            "com.emme.calendar.infrastructure",
            "com.emme.calendar.web");

    assertThat(CLASSES.stream().map(JavaClass::getPackageName))
        .noneMatch(
            packageName ->
                legacyPrefixes.contains(packageName)
                    || packageName.startsWith("com.emme.calendar.infrastructure."));
  }

  @Test
  void publicContractsAreGroupedByKind() {
    assertThat(hasClass("com.emme.calendar.api.result.CalendarEventLinkInfo")).isTrue();
    assertThat(hasClass("com.emme.calendar.api.usecase.GetBusyTimesUseCase")).isTrue();
    assertThat(hasClass("com.emme.calendar.api.usecase.SyncCalendarEventsUseCase")).isTrue();
    assertThat(hasClass("com.emme.calendar.api.usecase.FindCalendarEventLinksUseCase")).isTrue();
    assertThat(hasClass("com.emme.calendar.api.type.TokenSource")).isTrue();
    assertThat(hasClass("com.emme.calendar.api.event.CalendarSyncRequested")).isTrue();
  }

  @Test
  void webControllersAreGroupedInTheControllerPackage() {
    assertThat(hasClass("com.emme.calendar.adapter.in.web.controller.CalendarController")).isTrue();
    assertThat(hasClass("com.emme.calendar.adapter.in.web.controller.ClientCalendarController"))
        .isTrue();
    assertThat(hasClass("com.emme.calendar.adapter.in.web.controller.GoogleOAuthController"))
        .isTrue();
    assertThat(CLASSES.stream())
        .filteredOn(
            javaClass -> javaClass.getPackageName().equals("com.emme.calendar.adapter.in.web"))
        .noneMatch(javaClass -> javaClass.getSimpleName().endsWith("Controller"));
  }

  @Test
  void webResponsesHaveDedicatedFilesOutsideControllers() throws IOException {
    assertThat(
            Files.exists(
                SOURCE_ROOT.resolve("adapter/in/web/response/CalendarBusyTimeResponse.java")))
        .isTrue();
    assertThat(
            Files.exists(
                SOURCE_ROOT.resolve("adapter/in/web/response/CalendarSyncStateResponse.java")))
        .isTrue();
    String controller =
        Files.readString(SOURCE_ROOT.resolve("adapter/in/web/controller/CalendarController.java"));
    assertThat(controller).doesNotContain("record TimeRangeResponse");
    assertThat(controller).doesNotContain("record SyncStateResponse");
  }

  @Test
  void clientCalendarControllerUsesApplicationContracts() throws IOException {
    String controller =
        Files.readString(
            SOURCE_ROOT.resolve("adapter/in/web/controller/ClientCalendarController.java"));

    assertThat(controller).doesNotContain("com.emme.calendar.adapter.out");
    assertThat(Files.exists(SOURCE_ROOT.resolve("api/usecase/SyncClientCalendarUseCase.java")))
        .isTrue();
    assertThat(Files.exists(SOURCE_ROOT.resolve("api/usecase/UnsyncClientCalendarUseCase.java")))
        .isTrue();
  }

  @Test
  void googleOAuthControllerUsesApplicationContracts() throws IOException {
    String controller =
        Files.readString(
            SOURCE_ROOT.resolve("adapter/in/web/controller/GoogleOAuthController.java"));

    assertThat(controller).doesNotContain("com.emme.calendar.adapter.out");
    assertThat(Files.exists(SOURCE_ROOT.resolve("application/port/out/GoogleOAuthPort.java")))
        .isTrue();
    assertThat(Files.exists(SOURCE_ROOT.resolve("api/type/GoogleOAuthPersona.java"))).isTrue();
  }

  @Test
  void everyMaterializedApplicationPackageHasPackageMetadata() {
    assertThat(Files.exists(SOURCE_ROOT.resolve("application/mapper/package-info.java"))).isTrue();
  }

  @Test
  void applicationServicesAreOneUseCasePerClass() {
    Set<String> legacyServices =
        Set.of("CalendarService", "CalendarSyncApiService", "CalendarSyncApi");

    assertThat(CLASSES.stream())
        .extracting(JavaClass::getSimpleName)
        .doesNotContainAnyElementsOf(legacyServices);
    assertThat(hasClass("com.emme.calendar.application.service.GetBusyTimesService")).isTrue();
    assertThat(hasClass("com.emme.calendar.application.service.SyncCalendarEventsService"))
        .isTrue();
  }

  @Test
  void applicationServicesDoNotDependOnOutboundAdapterImplementations() {
    noClasses()
        .that()
        .resideInAnyPackage("com.emme.calendar.application.service..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("com.emme.calendar.adapter.out..")
        .check(CLASSES);
  }

  @Test
  void calendarAdaptersUseTypedConfigurationInsteadOfValueInjection() throws IOException {
    assertThat(
            Files.readString(SOURCE_ROOT.resolve("application/service/GetBusyTimesService.java")))
        .doesNotContain("@Value(");
    assertThat(
            Files.readString(
                SOURCE_ROOT.resolve("adapter/out/google/adapter/StaffCalendarSyncAdapter.java")))
        .doesNotContain("@Value(");
    assertThat(Files.exists(SOURCE_ROOT.resolve("configuration/CalendarProperties.java"))).isTrue();
    assertThat(
            Files.readString(
                SOURCE_ROOT.resolve("adapter/out/google/client/GoogleCalendarClient.java")))
        .doesNotContain("System.getenv(");
    assertThat(Files.exists(SOURCE_ROOT.resolve("configuration/GoogleCalendarProperties.java")))
        .isTrue();
  }

  @Test
  void googleAdaptersDoNotConstructTransportDependenciesInternally() throws IOException {
    try (var paths = Files.walk(SOURCE_ROOT.resolve("adapter/out/google"))) {
      paths
          .filter(path -> path.toString().endsWith(".java"))
          .forEach(
              path -> {
                try {
                  assertThat(Files.readString(path))
                      .as("Google adapter source %s", path)
                      .doesNotContain("new OkHttpClient(");
                } catch (IOException exception) {
                  throw new IllegalStateException("Cannot inspect " + path, exception);
                }
              });
    }
    assertThat(Files.exists(SOURCE_ROOT.resolve("configuration/GoogleClientConfiguration.java")))
        .isTrue();
  }

  private static boolean hasClass(String className) {
    return CLASSES.stream().anyMatch(javaClass -> javaClass.getName().equals(className));
  }

  private static Path sourcePath(String relativePath) {
    Path current = Path.of("").toAbsolutePath();
    for (int attempt = 0; attempt < 8 && current != null; attempt++) {
      if (Files.exists(current.resolve("settings.gradle.kts"))) {
        return current.resolve(relativePath);
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate repository source: " + relativePath);
  }
}
