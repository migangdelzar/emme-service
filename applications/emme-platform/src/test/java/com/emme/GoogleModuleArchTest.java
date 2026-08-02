package com.emme;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Architectural boundary verification for the Google Workspace module.
 *
 * <p>Verifies:
 *
 * <ul>
 *   <li>Calendar module does <b>not</b> depend on google module (one-way dependency)
 *   <li>Calendar Google adapters follow the canonical outbound package conventions
 * </ul>
 *
 * <p>Note: Spring Modulith overall module verification is handled by {@link ModularityTest}.
 */
class GoogleModuleArchTest {

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.emme");

  private static final Set<String> ALLOWED_GOOGLE_SUB_MODULES =
      Set.of("adapter", "client", "provider", "model");

  // ── One-way dependency: calendar must not depend on google ───────────────

  @Test
  void calendarModuleDoesNotDependOnGoogle() {
    if (CLASSES.stream().noneMatch(c -> c.getPackageName().startsWith("com.emme.google."))) {
      return;
    }
    noClasses()
        .that()
        .resideInAnyPackage("com.emme.calendar..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("com.emme.google..")
        .because(
            "Calendar module must not depend on Google module. "
                + "The dependency is one-way: google → calendar.")
        .check(CLASSES);
  }

  // ── Google module internal structure ────────────────────────────────────

  @Test
  void googleModuleUsesExpectedSubPackages() {
    var packages =
        CLASSES.stream()
            .map(c -> c.getPackageName())
            .filter(
                pkg ->
                    pkg.startsWith("com.emme.calendar.adapter.out.google.")
                        || pkg.startsWith("com.emme.google."))
            .distinct()
            .sorted()
            .toList();

    if (packages.isEmpty()) {
      return;
    }

    for (String pkg : packages) {
      String remainder =
          pkg.startsWith("com.emme.calendar.adapter.out.google.")
              ? pkg.substring("com.emme.calendar.adapter.out.google.".length())
              : pkg.substring("com.emme.google.".length());
      String topLevel = remainder.split("\\.")[0];
      assertThat(ALLOWED_GOOGLE_SUB_MODULES)
          .as("Package %s must be under a known google sub-module", pkg)
          .contains(topLevel);
    }
  }
}
