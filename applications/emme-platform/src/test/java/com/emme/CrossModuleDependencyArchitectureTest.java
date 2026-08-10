package com.emme;

import static com.emme.testing.architecture.ModuleDependencyRules.crossModuleImplementationDependenciesAreForbidden;

import com.emme.testing.architecture.ArchitectureTestSupport;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.Test;

/** Verifies that business modules communicate through public API contracts only. */
class CrossModuleDependencyArchitectureTest {

  private static final JavaClasses CLASSES = ArchitectureTestSupport.productionClasses("com.emme");

  @Test
  void crossModuleDependenciesUsePublicApis() {
    crossModuleImplementationDependenciesAreForbidden().check(CLASSES);
  }
}
