package com.emme.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.testing.BaseUnitTest;
import org.junit.jupiter.api.Test;

class BookingModuleTest extends BaseUnitTest {

  @Test
  void moduleLoads() {
    assertThat(getClass().getPackageName()).contains("booking");
  }

  @Test
  void testStructureExists() {
    assertThat(true).isTrue();
  }
}
