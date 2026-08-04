package com.emme.testing.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;

/** Reusable DDD and Hexagonal dependency rules for Emme production modules. */
public final class DddHexagonalRules {

  private DddHexagonalRules() {}

  /** Domain code remains independent of frameworks, transports, and adapters. */
  public static ArchRule domainLayerMustBeFrameworkFree() {
    return noClasses()
        .that()
        .resideInAnyPackage("com.emme.*.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "org.hibernate..",
            "com.fasterxml.jackson..",
            "org.apache.kafka..",
            "liquibase..",
            "com.zaxxer.hikari..",
            "com.emme.*.adapter..")
        .because("domain models and policies must not depend on infrastructure")
        .allowEmptyShould(true);
  }

  /** Application orchestration depends on ports and domain contracts, not adapters. */
  public static ArchRule applicationLayerMustNotDependOnAdapters() {
    return noClasses()
        .that()
        .resideInAnyPackage("com.emme.*.application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "com.emme.*.adapter..",
            "jakarta.persistence..",
            "org.hibernate..",
            "org.springframework.data..",
            "org.springframework.kafka..",
            "com.zaxxer.hikari..")
        .because("application services must depend on ports instead of technical adapters")
        .allowEmptyShould(true);
  }

  /** Inbound adapters translate input and never reach directly into outbound technology. */
  public static ArchRule inboundAdaptersMustNotDependOnOutboundAdapters() {
    return noClasses()
        .that()
        .resideInAnyPackage("com.emme.*.adapter.in..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("com.emme.*.adapter.out..")
        .because("inbound adapters must invoke use cases rather than technical integrations")
        .allowEmptyShould(true);
  }

  /** Public contracts do not leak domain, application, or adapter implementation types. */
  public static ArchRule publicApiMustNotDependOnImplementation() {
    return noClasses()
        .that()
        .resideInAnyPackage("com.emme.*.api..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "com.emme.*.domain..", "com.emme.*.application..", "com.emme.*.adapter..")
        .because("module APIs must expose stable contracts rather than implementation types")
        .allowEmptyShould(true);
  }

  /** JPA entities stay inside the owning module's outbound persistence adapter. */
  public static ArchRule persistenceEntitiesMustBeOutbound() {
    return classes()
        .that()
        .areAnnotatedWith(Entity.class)
        .should()
        .resideInAnyPackage("com.emme..adapter.out.persistence.entity..", "com.emme.shared..")
        .because("persistence representations must not become domain or API types")
        .allowEmptyShould(true);
  }
}
