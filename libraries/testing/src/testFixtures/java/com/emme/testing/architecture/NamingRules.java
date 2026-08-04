package com.emme.testing.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

/** Reusable naming rules for the canonical DDD and Hexagonal package types. */
public final class NamingRules {

  private NamingRules() {}

  /** HTTP entry points have an explicit Controller suffix. */
  public static ArchRule controllersUseControllerSuffix() {
    return classes()
        .that()
        .areAnnotatedWith(RestController.class)
        .or()
        .areAnnotatedWith(Controller.class)
        .should(useSuffix("Controller"))
        .because("HTTP entry points are named explicitly as controllers");
  }

  /** One-use-case application services have an explicit Service suffix. */
  public static ArchRule applicationServicesUseServiceSuffix() {
    return classes()
        .that()
        .resideInAnyPackage("com.emme..application.service..")
        .should(
            useSuffixExcept(
                "Service", "Support", "ApplicationSupport", "BoundaryTest", "Configuration"))
        .because("application orchestration types use the Service suffix");
  }

  /** Application mappers are named as mappers. */
  public static ArchRule applicationMappersUseMapperSuffix() {
    return classes()
        .that()
        .resideInAnyPackage("com.emme..application.mapper..")
        .should(useSuffix("Mapper"))
        .because("application mapping is represented by explicitly named mappers");
  }

  /** Persistence adapters are named as adapters. */
  public static ArchRule persistenceAdaptersUseAdapterSuffix() {
    return classes()
        .that()
        .resideInAnyPackage("com.emme..adapter.out.persistence.adapter..")
        .should(useSuffix("Adapter"))
        .because("persistence adapters implement application-owned ports");
  }

  /** Persistence mappers are named as mappers. */
  public static ArchRule persistenceMappersUseMapperSuffix() {
    return classes()
        .that()
        .resideInAnyPackage("com.emme..adapter.out.persistence.mapper..")
        .should(useSuffix("Mapper"))
        .because("persistence mapping stays at the outbound boundary");
  }

  /** API commands are records with the Command suffix. */
  public static ArchRule commandsUseCommandSuffix() {
    return classes()
        .that()
        .resideInAnyPackage("com.emme..api.command..")
        .should(useRecordAndSuffix("Command"))
        .because("commands are immutable public intentions");
  }

  /** API queries are records with the Query suffix. */
  public static ArchRule queriesUseQuerySuffix() {
    return classes()
        .that()
        .resideInAnyPackage("com.emme..api.query..")
        .should(useRecordAndSuffix("Query"))
        .because("queries are immutable public read requests");
  }

  /** Inbound ports use the UseCase suffix. */
  public static ArchRule useCasesUseUseCaseSuffix() {
    return classes()
        .that()
        .resideInAnyPackage("com.emme..api.usecase..")
        .should(useSuffix("UseCase"))
        .because("inbound ports are explicit use-case contracts");
  }

  /** Public API exceptions use the Exception suffix. */
  public static ArchRule exceptionsUseExceptionSuffix() {
    return classes()
        .that()
        .resideInAnyPackage("com.emme..api.exception..")
        .should(useSuffix("Exception"))
        .because("public API failures use stable exception names");
  }

  /** Public result records use a semantic name instead of the ambiguous Info suffix. */
  public static ArchRule apiResultsAvoidInfoSuffix() {
    return classes()
        .that()
        .resideInAnyPackage("com.emme..api.result..")
        .should(notContainToken("Info"))
        .because("public result records must describe their representation explicitly");
  }

  /** Public API vocabulary uses the concept name directly instead of the ambiguous View suffix. */
  public static ArchRule apiTypesAvoidViewSuffix() {
    return classes()
        .that()
        .resideInAnyPackage("com.emme..api.type..")
        .should(notContainToken("View"))
        .because("public vocabulary types are not transport-specific views");
  }

  /** Public API types are stable value vocabulary, not implementation ports. */
  public static ArchRule apiTypesAreRecordsOrEnums() {
    return classes()
        .that()
        .resideInAnyPackage("com.emme..api.type..")
        .should(
            new ArchCondition<>("be records or enums") {
              @Override
              public void check(JavaClass javaClass, ConditionEvents events) {
                if (!javaClass.getSimpleName().equals("package-info")
                    && !javaClass.isRecord()
                    && !javaClass.isEnum()) {
                  events.add(
                      SimpleConditionEvent.violated(
                          javaClass,
                          javaClass.getName()
                              + " must be a record or enum in the public vocabulary package"));
                }
              }
            })
        .because("interfaces and framework ports belong to application or adapter packages")
        .allowEmptyShould(true);
  }

  private static ArchCondition<JavaClass> useSuffix(String suffix) {
    return useSuffixExcept(suffix);
  }

  private static ArchCondition<JavaClass> useSuffixExcept(String suffix, String... exceptions) {
    return new ArchCondition<>("have simple names ending in " + suffix) {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        if (javaClass.isAnonymousClass()) {
          return;
        }
        String name = javaClass.getSimpleName();
        if (name.equals("package-info")) {
          return;
        }
        for (String exception : exceptions) {
          if (name.endsWith(exception)) {
            return;
          }
        }
        if (!name.endsWith(suffix)) {
          events.add(
              SimpleConditionEvent.violated(
                  javaClass, javaClass.getName() + " must end with " + suffix));
        }
      }
    };
  }

  private static ArchCondition<JavaClass> useRecordAndSuffix(String suffix) {
    return new ArchCondition<>("be records ending in " + suffix) {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        String name = javaClass.getSimpleName();
        if (name.equals("package-info")) {
          return;
        }
        if (!javaClass.isRecord() || !name.endsWith(suffix)) {
          events.add(
              SimpleConditionEvent.violated(
                  javaClass, javaClass.getName() + " must be a record ending with " + suffix));
        }
      }
    };
  }

  private static ArchCondition<JavaClass> notContainToken(String token) {
    return new ArchCondition<>("not contain the ambiguous token " + token) {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        String name = javaClass.getSimpleName();
        if (name.equals("package-info")) {
          return;
        }
        if (name.contains(token)) {
          events.add(
              SimpleConditionEvent.violated(
                  javaClass, javaClass.getName() + " must not contain " + token));
        }
      }
    };
  }
}
