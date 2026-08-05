package com.emme.staffing;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.testing.BaseUnitTest;
import org.junit.jupiter.api.Test;

class StaffingModuleTest extends BaseUnitTest {

  @Test
  void moduleLoads() {
    assertThat(getClass().getPackageName()).contains("staffing");
  }

  @Test
  void testStructureExists() {
    assertThat(true).isTrue();
  }
}
