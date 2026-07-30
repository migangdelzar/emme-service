package com.emme;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Package conventions of the current module structure (see
 * docs/superpowers/specs/2026-07-08-architecture-test-suite-design.md §4.3).
 */
class LayerConventionTest {

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.emme");

  private static Set<String> domainModules() {
    return CLASSES.stream()
        .map(JavaClass::getPackageName)
        .filter(pkg -> pkg.startsWith("com.emme."))
        .map(pkg -> pkg.substring("com.emme.".length()).split("\\.")[0])
        .filter(module -> !module.equals("shared"))
        .collect(Collectors.toSet());
  }

  @Test
  void persistenceTypesResideInEntityPackages() {
    classes()
        .that()
        .areAnnotatedWith(jakarta.persistence.Entity.class)
        .or()
        .areAnnotatedWith(jakarta.persistence.Embeddable.class)
        .or()
        .areAnnotatedWith(jakarta.persistence.MappedSuperclass.class)
        .or()
        .areAssignableTo(org.springframework.data.repository.Repository.class)
        .should()
        .resideInAnyPackage("com.emme.*.entity..", "com.emme.shared..")
        .because(
            "persistence types live in <module>/entity (shared holds base mapped superclasses)")
        .check(CLASSES);
  }

  @Test
  void controllersResideInWebPackages() {
    classes()
        .that()
        .areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
        .or()
        .areAnnotatedWith(org.springframework.stereotype.Controller.class)
        .should()
        .resideInAPackage("..web..")
        .allowEmptyShould(true)
        .check(CLASSES);
  }

  @Test
  void configurationsResideInConfigPackages() {
    classes()
        .that()
        .areAnnotatedWith(org.springframework.context.annotation.Configuration.class)
        .should()
        .resideInAPackage("..config..")
        .allowEmptyShould(true)
        .check(CLASSES);
  }

  @Test
  void domainPackagesDoNotImportFrameworkTypes() {
    noClasses()
        .that()
        .resideInAnyPackage("com.emme.*.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "org.hibernate..",
            "org.springframework.data.redis..")
        .because("Domain packages must not import framework types (C-039)")
        .allowEmptyShould(true)
        .check(CLASSES);
  }

  @Test
  void moduleEntitiesAndRepositoriesAreModulePrivate() {
    Set<String> modules = domainModules();
    assertThat(modules).isNotEmpty();
    for (String module : modules) {
      String modulePackage = "com.emme." + module + "..";
      String entityPackage = "com.emme." + module + ".entity..";
      noClasses()
          .that()
          .resideOutsideOfPackage(modulePackage)
          .should()
          .dependOnClassesThat(
              resideInAPackage(entityPackage)
                  .and(
                      DescribedPredicate.or(
                          annotatedWith(jakarta.persistence.Entity.class),
                          assignableTo(org.springframework.data.repository.Repository.class))))
          .because(
              "cross-module data access must go through the owning module's API, not "
                  + module
                  + "'s entities/repositories")
          .allowEmptyShould(true)
          .check(CLASSES);
    }
  }
}
