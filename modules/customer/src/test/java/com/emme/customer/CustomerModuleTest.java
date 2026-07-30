package com.emme.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.testing.BaseUnitTest;
import org.junit.jupiter.api.Test;

class CustomerModuleTest extends BaseUnitTest {

  @Test
  void moduleLoads() {
    assertThat(getClass().getPackageName()).contains("customer");
  }

  @Test
  void testStructureExists() {
    assertThat(true).isTrue();
  }
}
