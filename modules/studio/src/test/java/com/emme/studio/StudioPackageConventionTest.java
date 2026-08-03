package com.emme.studio;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
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
    assertThat(hasClass("com.emme.studio.api.usecase.GetBusinessProfileUseCase")).isTrue();
    assertThat(hasClass("com.emme.studio.api.usecase.ListAppointmentsUseCase")).isTrue();
    assertThat(hasClass("com.emme.studio.api.usecase.ListCustomersUseCase")).isTrue();
    assertThat(hasClass("com.emme.studio.api.event.DashboardEvent")).isTrue();
  }

  @Test
  void publicUseCasesDoNotExposeApplicationResultTypes() {
    assertThat(hasClass("com.emme.studio.api.result.AppointmentDetails")).isTrue();
    assertThat(hasClass("com.emme.studio.api.result.AvailableSlot")).isTrue();
    assertThat(CLASSES.stream())
        .filteredOn(javaClass -> javaClass.getPackageName().startsWith("com.emme.studio.api"))
        .noneMatch(
            javaClass -> javaClass.getName().startsWith("com.emme.studio.application.result."));
  }

  @Test
  void applicationServicesAreOneUseCasePerClass() {
    Set<String> legacyServices =
        Set.of(
            "AppointmentService",
            "ArtistService",
            "BusinessConfigService",
            "CustomerService",
            "ServiceCatalogService",
            "SlotSearchService",
            "SalonApiImpl");

    assertThat(CLASSES.stream())
        .extracting(JavaClass::getSimpleName)
        .doesNotContainAnyElementsOf(legacyServices);
    assertThat(hasClass("com.emme.studio.application.service.CreateCustomerService")).isTrue();
    assertThat(hasClass("com.emme.studio.application.service.ListCustomersService")).isTrue();
    assertThat(hasClass("com.emme.studio.application.service.CreateAppointmentService")).isTrue();
    assertThat(hasClass("com.emme.studio.application.service.FindAvailableSlotsService")).isTrue();
  }

  @Test
  void applicationLayerDoesNotDependOnOutboundAdapters() {
    noClasses()
        .that()
        .resideInAnyPackage("com.emme.studio.application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("com.emme.studio.adapter.out..")
        .check(CLASSES);
  }

  @Test
  void applicationLayerDoesNotDependOnPersistenceExceptions() {
    noClasses()
        .that()
        .resideInAnyPackage("com.emme.studio.application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("jakarta.persistence..")
        .because("application failures must use Studio-owned exceptions")
        .check(CLASSES);
    assertThat(hasClass("com.emme.studio.api.exception.StudioResourceNotFoundException")).isTrue();
  }

  private static boolean hasClass(String className) {
    return CLASSES.stream().anyMatch(javaClass -> javaClass.getName().equals(className));
  }
}
