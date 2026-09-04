package com.emme.shared.persistence.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BootstrapConnectionExecutorNamingTest {

  private static final Path SOURCE_ROOT = Path.of("src/main/java/com/emme/shared/persistence/jdbc");

  @Test
  void namesTheConnectionCallbackForItsBootstrapOnlyPurpose() throws Exception {
    assertThat(Files.exists(SOURCE_ROOT.resolve("BootstrapConnectionExecutor.java"))).isTrue();
    assertThat(Files.exists(SOURCE_ROOT.resolve("JdbcConnectionExecutor.java"))).isFalse();
  }
}
