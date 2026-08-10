package com.emme.testing.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;

/** Reusable shape rules for public module event contracts. */
public final class EventContractRules {

  private static final Set<String> IMPERATIVE_SUFFIXES =
      Set.of("Command", "Query", "Request", "UseCase", "Result", "Exception");

  private EventContractRules() {}

  /** Public event contracts are immutable Java records. */
  public static ArchRule publicEventsMustBeRecords() {
    return classes()
        .that()
        .resideInAnyPackage("com.emme..api.event..")
        .should(
            new ArchCondition<>("be immutable records") {
              @Override
              public void check(JavaClass javaClass, ConditionEvents events) {
                if (!javaClass.isRecord() && !javaClass.getSimpleName().equals("package-info")) {
                  events.add(
                      SimpleConditionEvent.violated(
                          javaClass, javaClass.getName() + " must be declared as a record"));
                }
              }
            })
        .because("public events are immutable facts, not mutable implementation objects")
        .allowEmptyShould(true);
  }

  /** Public event names do not masquerade as commands, queries, or transport requests. */
  public static ArchRule publicEventNamesMustDescribeFacts() {
    return classes()
        .that()
        .resideInAnyPackage("com.emme..api.event..")
        .should(
            new ArchCondition<>("use fact-oriented names") {
              @Override
              public void check(JavaClass javaClass, ConditionEvents events) {
                String name = javaClass.getSimpleName();
                if (name.equals("package-info")) {
                  return;
                }
                for (String suffix : IMPERATIVE_SUFFIXES) {
                  if (name.endsWith(suffix)) {
                    events.add(
                        SimpleConditionEvent.violated(
                            javaClass,
                            javaClass.getName()
                                + " must not use imperative/public-result suffix "
                                + suffix));
                    return;
                  }
                }
              }
            })
        .because("api.event contains facts that already happened, not caller intentions")
        .allowEmptyShould(true);
  }

  /** Public facts do not repeat their package meaning in the type name. */
  public static ArchRule publicEventNamesAvoidEventSuffix() {
    return classes()
        .that()
        .resideInAnyPackage("com.emme..api.event..")
        .should(
            new ArchCondition<>("avoid the redundant Event suffix") {
              @Override
              public void check(JavaClass javaClass, ConditionEvents events) {
                String name = javaClass.getSimpleName();
                if (!name.equals("package-info") && name.endsWith("Event")) {
                  events.add(
                      SimpleConditionEvent.violated(
                          javaClass,
                          javaClass.getName()
                              + " must omit the redundant Event suffix inside api.event"));
                }
              }
            })
        .because("the api.event package already identifies the type as a public event")
        .allowEmptyShould(true);
  }
}
