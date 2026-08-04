package com.emme;

import static com.emme.testing.architecture.DddHexagonalRules.applicationLayerMustNotDependOnAdapters;
import static com.emme.testing.architecture.DddHexagonalRules.domainLayerMustBeFrameworkFree;
import static com.emme.testing.architecture.DddHexagonalRules.inboundAdaptersMustNotDependOnOutboundAdapters;
import static com.emme.testing.architecture.DddHexagonalRules.publicApiMustNotDependOnImplementation;
import static org.assertj.core.api.Assertions.assertThat;

import com.emme.testing.architecture.ArchitectureTestSupport;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.Test;

/** Executes reusable DDD and Hexagonal architecture rules for the service. */
class DddHexagonalArchitectureTest {

  private static final JavaClasses CLASSES = ArchitectureTestSupport.productionClasses("com.emme");

  @Test
  void domainLayerIsFrameworkFree() {
    domainLayerMustBeFrameworkFree().check(CLASSES);
  }

  @Test
  void applicationLayerDoesNotDependOnAdapters() {
    applicationLayerMustNotDependOnAdapters().check(CLASSES);
  }

  @Test
  void inboundAdaptersDoNotDependOnOutboundAdapters() {
    inboundAdaptersMustNotDependOnOutboundAdapters().check(CLASSES);
  }

  @Test
  void publicApiDoesNotDependOnImplementationPackages() {
    publicApiMustNotDependOnImplementation().check(CLASSES);
  }

  @Test
  void architectureFixtureImportsProductionClasses() {
    assertThat(CLASSES).isNotEmpty();
  }
}
