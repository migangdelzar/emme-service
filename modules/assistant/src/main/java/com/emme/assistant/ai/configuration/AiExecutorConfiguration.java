package com.emme.assistant.ai.configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root for AI infrastructure executors.
 *
 * <p>Blocking model and vector I/O uses virtual threads. Bounded platform pools are reserved for
 * CPU-bound or background work so that those workloads cannot exhaust the I/O executor. The
 * application does not replace the common ForkJoinPool.
 */
@Configuration(proxyBeanMethods = false)
public class AiExecutorConfiguration {

  @Bean(name = "aiIoExecutor", destroyMethod = "close")
  public ExecutorService aiIoExecutor() {
    return Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("emme-ai-io-", 0).factory());
  }

  @Bean(name = "aiBackgroundExecutor", destroyMethod = "close")
  public ExecutorService aiBackgroundExecutor(AiExecutorProperties properties) {
    return Executors.newFixedThreadPool(
        properties.backgroundParallelism(),
        Thread.ofPlatform().name("emme-ai-background-", 0).factory());
  }

  @Bean(name = "aiCpuExecutor", destroyMethod = "close")
  public ExecutorService aiCpuExecutor(AiExecutorProperties properties) {
    return Executors.newFixedThreadPool(
        properties.cpuParallelism(), Thread.ofPlatform().name("emme-ai-cpu-", 0).factory());
  }

  @Bean(name = "aiScheduler", destroyMethod = "close")
  public ScheduledExecutorService aiScheduler(AiExecutorProperties properties) {
    return Executors.newScheduledThreadPool(
        properties.schedulerPoolSize(),
        Thread.ofPlatform().name("emme-ai-scheduler-", 0).factory());
  }
}
