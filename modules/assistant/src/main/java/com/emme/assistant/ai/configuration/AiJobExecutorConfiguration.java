package com.emme.assistant.ai.configuration;

import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.adapter.out.event.SpringModulithAiJobPublisher;
import com.emme.assistant.ai.application.port.out.AiJobStatusStore;
import com.emme.assistant.ai.application.service.AiJobWorkerService;
import java.util.concurrent.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
@org.springframework.scheduling.annotation.EnableScheduling
@EnableConfigurationProperties(AiJobProperties.class)
public class AiJobExecutorConfiguration {
  @Bean
  SpringModulithAiJobPublisher aiJobPublisher(
      ApplicationEventPublisher events, AiJobStatusStore store) {
    return new SpringModulithAiJobPublisher(events, store);
  }

  @Bean
  AiJobWorkerService aiJobWorker(
      AiJobStatusStore store, ModelExecutionScheduler scheduler, AiJobProperties properties) {
    return new AiJobWorkerService(
        store,
        scheduler,
        (request, context) -> {
          throw new IllegalStateException("AI job handler disabled/deferred: " + request.type());
        },
        properties.maxAttempts());
  }

  @Bean(name = "aiJobExecutor", destroyMethod = "shutdown")
  ExecutorService aiJobExecutor(AiJobProperties p) {
    return new ThreadPoolExecutor(
        p.workerCount(),
        p.workerCount(),
        0,
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(p.queueCapacity()),
        Thread.ofPlatform().name("emme-ai-job-", 0).factory(),
        new ThreadPoolExecutor.AbortPolicy());
  }
}
