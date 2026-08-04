package com.emme;

import com.emme.testing.architecture.ArchitectureTestSupport;
import com.emme.testing.architecture.NamingRules;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.Test;

/** Verifies normalized names for the canonical DDD and Hexagonal package types. */
class NamingConventionArchitectureTest {

  private static final JavaClasses CLASSES = ArchitectureTestSupport.productionClasses("com.emme");

  @Test
  void controllersUseControllerSuffix() {
    NamingRules.controllersUseControllerSuffix().check(CLASSES);
  }

  @Test
  void applicationServicesUseServiceSuffix() {
    NamingRules.applicationServicesUseServiceSuffix().check(CLASSES);
  }

  @Test
  void applicationMappersUseMapperSuffix() {
    NamingRules.applicationMappersUseMapperSuffix().check(CLASSES);
  }

  @Test
  void persistenceAdaptersUseAdapterSuffix() {
    NamingRules.persistenceAdaptersUseAdapterSuffix().check(CLASSES);
  }

  @Test
  void persistenceMappersUseMapperSuffix() {
    NamingRules.persistenceMappersUseMapperSuffix().check(CLASSES);
  }

  @Test
  void apiTypesUseTheirPackageSuffix() {
    NamingRules.commandsUseCommandSuffix().check(CLASSES);
    NamingRules.queriesUseQuerySuffix().check(CLASSES);
    NamingRules.useCasesUseUseCaseSuffix().check(CLASSES);
    NamingRules.exceptionsUseExceptionSuffix().check(CLASSES);
  }

  @Test
  void apiResultsUseSemanticNames() {
    NamingRules.apiResultsAvoidInfoSuffix().check(CLASSES);
  }

  @Test
  void apiTypesUseDomainNamesInsteadOfTransportViews() {
    NamingRules.apiTypesAvoidViewSuffix().check(CLASSES);
  }
}
