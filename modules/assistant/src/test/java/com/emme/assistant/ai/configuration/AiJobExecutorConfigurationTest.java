package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;

class AiJobExecutorConfigurationTest {
  @Test
  void createsBoundedPlatformWorkerPool() throws Exception {
    ExecutorService executor =
        new AiJobExecutorConfiguration().aiJobExecutor(new AiJobProperties(1, 1, 3));
    try (executor) {
      assertThat(executor.submit(() -> Thread.currentThread().isVirtual()).get()).isFalse();
    }
  }
}
