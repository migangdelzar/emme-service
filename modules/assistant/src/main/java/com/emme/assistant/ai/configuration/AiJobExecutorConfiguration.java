package com.emme.assistant.ai.configuration;

import java.util.concurrent.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiJobProperties.class)
public class AiJobExecutorConfiguration {
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
