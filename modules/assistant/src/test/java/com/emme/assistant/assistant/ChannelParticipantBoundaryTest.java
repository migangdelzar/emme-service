package com.emme.assistant.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ChannelParticipantBoundaryTest {

  private static final Path ROOT = Path.of("src/main/java/com/emme/assistant");

  @Test
  void ownsChannelParticipantPersistenceBehindAnApplicationPort() throws Exception {
    assertThat(Files.exists(ROOT.resolve("application/port/out/ChannelParticipantRepository.java")))
        .isTrue();
    assertThat(
            Files.exists(
                ROOT.resolve(
                    "adapter/out/persistence/adapter/ChannelParticipantPersistenceAdapter.java")))
        .isTrue();
    assertThat(
            Files.readString(ROOT.resolve("application/service/ProcessWhatsAppMessageService.java"))
                .replace("\\r\\n", "\\n"))
        .doesNotContain("adapter.out.persistence.entity.ChannelParticipantEntity")
        .doesNotContain(
            "adapter.out.persistence.repository.SpringDataChannelParticipantRepository");
  }
}
