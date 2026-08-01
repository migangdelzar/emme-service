package com.emme.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TenantUseCaseBoundaryTest {

  private static final Path ROOT = Path.of("src/main/java/com/emme/tenancy");

  @Test
  void exposesFocusedTenantUseCasesInsteadOfMultiOperationServices() throws Exception {
    assertThat(Files.exists(ROOT.resolve("api/command/CreateTenantCommand.java"))).isTrue();
    assertThat(Files.exists(ROOT.resolve("api/usecase/CreateTenantUseCase.java"))).isTrue();
    assertThat(Files.exists(ROOT.resolve("application/service/CreateTenantService.java"))).isTrue();
    assertThat(Files.exists(ROOT.resolve("api/usecase/GetTenantUseCase.java"))).isTrue();
    assertThat(Files.exists(ROOT.resolve("application/service/GetTenantService.java"))).isTrue();
    assertThat(Files.exists(ROOT.resolve("application/service/TenantService.java"))).isFalse();
    assertThat(Files.exists(ROOT.resolve("api/usecase/TenantApi.java"))).isFalse();
  }
}
