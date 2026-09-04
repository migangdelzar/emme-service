package com.emme.tenancy.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class TenancyPersistenceBoundaryTest {

  private static final Path APPLICATION_SERVICE_ROOT =
      Path.of("src/main/java/com/emme/tenancy/application/service");

  @Test
  void applicationServicesDoNotDependOnBootstrapJdbc() throws IOException {
    try (Stream<Path> sourceFiles = Files.walk(APPLICATION_SERVICE_ROOT)) {
      sourceFiles
          .filter(path -> path.toString().endsWith(".java"))
          .forEach(
              path -> {
                try {
                  String source = Files.readString(path);
                  assertThat(source)
                      .as("application service must not own bootstrap JDBC: %s", path)
                      .doesNotContain("JdbcTemplate")
                      .doesNotContain("JdbcClient")
                      .doesNotContain("bootstrapJdbc");
                } catch (IOException exception) {
                  throw new IllegalStateException("Could not inspect " + path, exception);
                }
              });
    }
  }
}
