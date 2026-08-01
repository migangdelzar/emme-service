package com.emme.calendar;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CalendarPackageConventionTest {

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
    assertThat(hasClass("com.emme.calendar.api.usecase.CalendarSyncApi")).isTrue();
    assertThat(hasClass("com.emme.calendar.api.type.TokenSource")).isTrue();
    assertThat(hasClass("com.emme.calendar.api.event.CalendarSyncRequested")).isTrue();
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

  private static boolean hasClass(String className) {
    return CLASSES.stream().anyMatch(javaClass -> javaClass.getName().equals(className));
  }
}
