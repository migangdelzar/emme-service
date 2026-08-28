package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.Test;

class AiExecutorConfigurationTest {

  @Test
  void createsVirtualThreadsForBlockingAiIo() throws Exception {
    AiExecutorConfiguration configuration = new AiExecutorConfiguration();

    try (ExecutorService executor = configuration.aiIoExecutor()) {
      assertThat(executor.submit(() -> Thread.currentThread().isVirtual()).get()).isTrue();
    }
  }

  @Test
  void createsNamedPlatformThreadsForBoundedBackgroundWork() throws Exception {
    AiExecutorConfiguration configuration = new AiExecutorConfiguration();
    AiExecutorProperties properties = new AiExecutorProperties(3, 2, 1);

    try (ExecutorService executor = configuration.aiBackgroundExecutor(properties)) {
      String threadName = executor.submit(() -> Thread.currentThread().getName()).get();

      assertThat(threadName).startsWith("emme-ai-background-");
    }
  }

  @Test
  void createsAPlatformPoolForCpuWork() throws Exception {
    AiExecutorConfiguration configuration = new AiExecutorConfiguration();
    AiExecutorProperties properties = new AiExecutorProperties(3, 2, 1);

    try (ExecutorService executor = configuration.aiCpuExecutor(properties)) {
      assertThat(executor.submit(() -> Thread.currentThread().isVirtual()).get()).isFalse();
    }
  }

  @Test
  void createsTheConfiguredSchedulerPool() throws Exception {
    AiExecutorConfiguration configuration = new AiExecutorConfiguration();
    AiExecutorProperties properties = new AiExecutorProperties(3, 2, 1);

    try (ScheduledExecutorService executor = configuration.aiScheduler(properties)) {
      String threadName = executor.submit(() -> Thread.currentThread().getName()).get();

      assertThat(threadName).startsWith("emme-ai-scheduler-");
    }
  }

  @Test
  void rejectsNonPositivePoolSizes() {
    assertThatThrownBy(() -> new AiExecutorProperties(0, 2, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("backgroundParallelism must be greater than zero");
    assertThatThrownBy(() -> new AiExecutorProperties(2, 0, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("cpuParallelism must be greater than zero");
    assertThatThrownBy(() -> new AiExecutorProperties(2, 1, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("schedulerPoolSize must be greater than zero");
  }
}
