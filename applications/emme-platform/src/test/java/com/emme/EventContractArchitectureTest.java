package com.emme;

import com.emme.testing.architecture.ArchitectureTestSupport;
import com.emme.testing.architecture.EventContractRules;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.Test;

/** Verifies the shape of public module event contracts. */
class EventContractArchitectureTest {

  private static final JavaClasses CLASSES = ArchitectureTestSupport.productionClasses("com.emme");

  @Test
  void publicEventsAreImmutableRecords() {
    EventContractRules.publicEventsMustBeRecords().check(CLASSES);
  }

  @Test
  void publicEventsDoNotUseCommandOrQueryNames() {
    EventContractRules.publicEventNamesMustDescribeFacts().check(CLASSES);
  }
}
