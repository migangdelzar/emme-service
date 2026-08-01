package com.emme.studio;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Verifies the canonical package shape of the core Studio capability. */
class StudioPackageConventionTest {

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.emme.studio");

  @Test
  void coreProductionTypesDoNotRemainInLegacyRootPackages() {
    Set<String> legacyPrefixes =
        Set.of(
            "com.emme.studio.application",
            "com.emme.studio.entity",
            "com.emme.studio.event",
            "com.emme.studio.web");

    assertThat(CLASSES.stream().map(JavaClass::getPackageName)).noneMatch(legacyPrefixes::contains);
  }

  @Test
  void publicContractsAreGroupedByKind() {
    assertThat(hasClass("com.emme.studio.api.result.AppointmentInfo")).isTrue();
    assertThat(hasClass("com.emme.studio.api.result.BusinessProfileInfo")).isTrue();
    assertThat(hasClass("com.emme.studio.api.result.CustomerInfo")).isTrue();
    assertThat(hasClass("com.emme.studio.api.usecase.SalonApi")).isTrue();
    assertThat(hasClass("com.emme.studio.api.event.DashboardEvent")).isTrue();
  }

  private static boolean hasClass(String className) {
    return CLASSES.stream().anyMatch(javaClass -> javaClass.getName().equals(className));
  }
}
