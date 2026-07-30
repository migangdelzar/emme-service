package com.emme.workforce;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.testing.BaseUnitTest;
import org.junit.jupiter.api.Test;

class WorkforceModuleTest extends BaseUnitTest {

  @Test
  void moduleLoads() {
    assertThat(getClass().getPackageName()).contains("workforce");
  }

  @Test
  void testStructureExists() {
    assertThat(true).isTrue();
  }
}
