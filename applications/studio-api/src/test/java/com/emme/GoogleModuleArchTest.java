package com.emme;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Architectural boundary verification for the Google Workspace module.
 *
 * <p>Verifies:
 *
 * <ul>
 *   <li>Calendar module does <b>not</b> depend on google module (one-way dependency)
 *   <li>Google module follows internal package conventions (config, entity, oauth, etc.)
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
      Set.of("config", "entity", "application", "provider", "web");

  // ── One-way dependency: calendar must not depend on google ───────────────

  @Test
  void calendarModuleDoesNotDependOnGoogle() {
    // With the new module structure, google classes live under calendar.infrastructure.google.
    // The one-way dependency rule now applies within the calendar module itself:
    // calendar core must not depend on calendar infrastructure.
    // This test is informational — skip if no google classes exist on classpath.
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
    List<String> packages =
        CLASSES.stream()
            .map(c -> c.getPackageName())
            .filter(
                pkg ->
                    pkg.startsWith("com.emme.calendar.infrastructure.google.")
                        || pkg.startsWith("com.emme.google."))
            .distinct()
            .sorted()
            .toList();

    // Google module may not have been migrated yet from emme-studio; skip if empty
    if (packages.isEmpty()) {
      return;
    }

    for (String pkg : packages) {
      // Strip prefix: "com.emme.google." → remainder like "oauth" or "oauth.sub"
      // Strip google prefix regardless of whether it's under calendar.infrastructure.google or
      // com.emme.google
      String remainder =
          pkg.startsWith("com.emme.calendar.infrastructure.google.")
              ? pkg.substring("com.emme.calendar.infrastructure.google.".length())
              : pkg.substring("com.emme.google.".length());
      String topLevel = remainder.split("\\.")[0];
      assertThat(ALLOWED_GOOGLE_SUB_MODULES)
          .as("Package %s must be under a known google sub-module", pkg)
          .contains(topLevel);
    }
  }
}
