package com.emme;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTest {

  static final ApplicationModules modules = ApplicationModules.of(EmmeApplication.class);

  @Test
  void generateModuleDocumentation() {
    new Documenter(modules).writeDocumentation().writeIndividualModulesAsPlantUml();
  }

  @Test
  void moduleStructureIsValid() {
    modules.verify();
  }
}
