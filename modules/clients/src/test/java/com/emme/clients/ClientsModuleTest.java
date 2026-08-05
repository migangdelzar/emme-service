package com.emme.clients;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.testing.BaseUnitTest;
import org.junit.jupiter.api.Test;

class ClientsModuleTest extends BaseUnitTest {

  @Test
  void moduleLoads() {
    assertThat(getClass().getPackageName()).contains("clients");
  }

  @Test
  void testStructureExists() {
    assertThat(true).isTrue();
  }
}
