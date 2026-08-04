package com.emme.testing.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;

/** Reusable rules for dependencies between business modules. */
public final class ModuleDependencyRules {

  private static final String EMME_PACKAGE = "com.emme.";
  private static final Set<String> BUSINESS_MODULES =
      Set.of(
          "assistant",
          "audit",
          "booking",
          "calendar",
          "catalog",
          "customer",
          "identity",
          "notification",
          "payment",
          "studio",
          "tenancy",
          "workforce");
  private static final Set<String> IMPLEMENTATION_AREAS =
      Set.of("domain", "application", "adapter", "configuration");

  private ModuleDependencyRules() {}

  /**
   * Business modules may depend on another business module only through its public API package.
   *
   * <p>Technical packages such as Shared and Kernel are intentionally outside this rule. Their
   * ownership and public surfaces are governed by their own module metadata and rules.
   */
  public static ArchRule crossModuleImplementationDependenciesAreForbidden() {
    return classes()
        .that()
        .resideInAnyPackage("com.emme..")
        .should(
            new ArchCondition<>(
                "use only public API packages for cross-module business dependencies") {
              @Override
              public void check(JavaClass source, ConditionEvents events) {
                String sourceModule = businessModule(source.getPackageName());
                if (sourceModule == null) {
                  return;
                }

                for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                  JavaClass target = dependency.getTargetClass();
                  String targetModule = businessModule(target.getPackageName());
                  if (targetModule == null
                      || sourceModule.equals(targetModule)
                      || isPublicApi(target.getPackageName())) {
                    continue;
                  }

                  events.add(
                      SimpleConditionEvent.violated(
                          source,
                          source.getFullName()
                              + " depends on "
                              + target.getFullName()
                              + " across the "
                              + sourceModule
                              + " -> "
                              + targetModule
                              + " module boundary; use "
                              + targetModule
                              + ".api instead"));
                }
              }
            })
        .because("business modules must communicate through stable public API contracts")
        .allowEmptyShould(true);
  }

  private static boolean isPublicApi(String packageName) {
    String modulePath = packageName.substring(EMME_PACKAGE.length());
    return modulePath.equals("api")
        || modulePath.startsWith("api.")
        || modulePath.contains(".api.")
        || modulePath.endsWith(".api");
  }

  private static String businessModule(String packageName) {
    if (!packageName.startsWith(EMME_PACKAGE)) {
      return null;
    }
    String remainder = packageName.substring(EMME_PACKAGE.length());
    int separator = remainder.indexOf('.');
    String module = separator < 0 ? remainder : remainder.substring(0, separator);
    return BUSINESS_MODULES.contains(module) ? module : null;
  }
}
