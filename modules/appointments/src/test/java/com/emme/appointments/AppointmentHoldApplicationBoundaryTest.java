package com.emme.appointments;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AppointmentHoldApplicationBoundaryTest {

  @Test
  void exposesHoldCommandsUseCasesAndServicesInTheAppointmentsModule() {
    Path root = sourcePath("modules/appointments/src/main/java/com/emme/appointments");

    assertThat(Files.exists(root.resolve("api/command/CreateAppointmentHoldCommand.java")))
        .isTrue();
    assertThat(Files.exists(root.resolve("api/usecase/CreateAppointmentHoldUseCase.java")))
        .isTrue();
    assertThat(Files.exists(root.resolve("api/usecase/ReleaseAppointmentHoldUseCase.java")))
        .isTrue();
  }

  private static Path sourcePath(String relativePath) {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve(relativePath);
      if (Files.exists(candidate)) return candidate;
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate source path: " + relativePath);
  }
}
