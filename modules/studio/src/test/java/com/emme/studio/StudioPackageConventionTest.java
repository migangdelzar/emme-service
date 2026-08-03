package com.emme.studio;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.nio.file.Path;
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
    assertThat(hasClass("com.emme.studio.api.result.CustomerSummary")).isTrue();
    assertThat(hasClass("com.emme.studio.api.usecase.GetBusinessProfileUseCase")).isTrue();
    assertThat(hasClass("com.emme.studio.api.usecase.ListAppointmentsUseCase")).isTrue();
    assertThat(hasClass("com.emme.studio.api.usecase.ListCustomersUseCase")).isTrue();
    assertThat(hasClass("com.emme.studio.api.event.DashboardEvent")).isTrue();
  }

  @Test
  void webControllersAreGroupedInTheControllerPackageWithSemanticNames() {
    assertThat(hasClass("com.emme.studio.adapter.in.web.controller.AppointmentController"))
        .isTrue();
    assertThat(hasClass("com.emme.studio.adapter.in.web.controller.ArtistController")).isTrue();
    assertThat(
            hasClass("com.emme.studio.adapter.in.web.controller.BusinessConfigurationController"))
        .isTrue();
    assertThat(hasClass("com.emme.studio.adapter.in.web.controller.CustomerController")).isTrue();
    assertThat(hasClass("com.emme.studio.adapter.in.web.controller.DashboardController")).isTrue();
    assertThat(hasClass("com.emme.studio.adapter.in.web.controller.ServiceController")).isTrue();
    assertThat(CLASSES.stream())
        .filteredOn(
            javaClass -> javaClass.getPackageName().equals("com.emme.studio.adapter.in.web"))
        .noneMatch(javaClass -> javaClass.getSimpleName().endsWith("Controller"));
  }

  @Test
  void serviceWebContractsHaveDedicatedFiles() throws Exception {
    Path root = sourcePath("modules/studio/src/main/java/com/emme/studio");
    assertThat(
            java.nio.file.Files.exists(
                root.resolve("adapter/in/web/response/ServiceResponse.java")))
        .isTrue();
    assertThat(
            java.nio.file.Files.exists(
                root.resolve("adapter/in/web/request/CreateServiceRequest.java")))
        .isTrue();
    assertThat(
            java.nio.file.Files.exists(
                root.resolve("adapter/in/web/request/UpdateServiceRequest.java")))
        .isTrue();
    String controller =
        java.nio.file.Files.readString(
            root.resolve("adapter/in/web/controller/ServiceController.java"));
    assertThat(controller).doesNotContain("public record ServiceResponse");
    assertThat(controller).doesNotContain("public record CreateServiceRequest");
    assertThat(controller).doesNotContain("public record UpdateServiceRequest");
  }

  @Test
  void customerWebBoundaryDoesNotExposeDomainModels() throws Exception {
    Path root = sourcePath("modules/studio/src/main/java/com/emme/studio");
    String controller =
        java.nio.file.Files.readString(
            root.resolve("adapter/in/web/controller/CustomerController.java"));

    assertThat(controller).doesNotContain("com.emme.studio.domain.model");
    assertThat(java.nio.file.Files.exists(root.resolve("api/result/CustomerDetails.java")))
        .isTrue();
    assertThat(
            java.nio.file.Files.exists(
                root.resolve("adapter/in/web/response/CustomerResponse.java")))
        .isTrue();
  }

  @Test
  void serviceCatalogWebBoundaryDoesNotExposeDomainModels() throws Exception {
    Path root = sourcePath("modules/studio/src/main/java/com/emme/studio");
    String controller =
        java.nio.file.Files.readString(
            root.resolve("adapter/in/web/controller/ServiceController.java"));
    String response =
        java.nio.file.Files.readString(
            root.resolve("adapter/in/web/response/ServiceResponse.java"));

    assertThat(controller).doesNotContain("com.emme.studio.domain.model");
    assertThat(response).doesNotContain("com.emme.studio.domain.model");
    assertThat(java.nio.file.Files.exists(root.resolve("api/result/ServiceDetails.java"))).isTrue();
  }

  @Test
  void businessConfigurationContractsDoNotExposeDomainModels() throws Exception {
    Path root = sourcePath("modules/studio/src/main/java/com/emme/studio");
    String useCase =
        java.nio.file.Files.readString(root.resolve("api/usecase/GetOperatingHoursUseCase.java"));
    String controller =
        java.nio.file.Files.readString(
            root.resolve("adapter/in/web/controller/BusinessConfigurationController.java"));

    assertThat(useCase).doesNotContain("com.emme.studio.domain.model");
    assertThat(controller).doesNotContain("com.emme.studio.domain.model");
    assertThat(java.nio.file.Files.exists(root.resolve("api/result/OperatingHoursDetails.java")))
        .isTrue();
    assertThat(java.nio.file.Files.exists(root.resolve("api/type/BusinessDay.java"))).isTrue();
  }

  @Test
  void artistWebBoundaryDoesNotExposeDomainModels() throws Exception {
    Path root = sourcePath("modules/studio/src/main/java/com/emme/studio");
    String controller =
        java.nio.file.Files.readString(
            root.resolve("adapter/in/web/controller/ArtistController.java"));

    assertThat(controller).doesNotContain("com.emme.studio.domain.model");
    assertThat(java.nio.file.Files.exists(root.resolve("api/result/ArtistDetails.java"))).isTrue();
    assertThat(java.nio.file.Files.exists(root.resolve("api/result/ArtistCapabilityDetails.java")))
        .isTrue();
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

  private static Path sourcePath(String relativePath) {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve(relativePath);
      if (java.nio.file.Files.exists(candidate)) {
        return candidate;
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate source path: " + relativePath);
  }
}
