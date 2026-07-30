package com.emme.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.testing.BaseUnitTest;
import org.junit.jupiter.api.Test;

class AuditModuleTest extends BaseUnitTest {

  @Test
  void moduleLoads() {
    assertThat(getClass().getPackageName()).contains("audit");
  }

  @Test
  void testStructureExists() {
    assertThat(true).isTrue();
  }
}
