package com.example.concurrentorchestrator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class OrchestrationConfig {

  @Value("${api.orchestration.apiPool}") private int apiParallelism;
  @Value("${api.orchestration.dbPool}") private int dbParallelism;

  @Bean("apiExecutor")
  public Executor getApiOrchestratorExecutor() {
    return Executors.newFixedThreadPool(apiParallelism, new CustomizableThreadFactory("apiWorker-"));
  }

  @Bean("dbExecutor")
  public Executor getDbOrchestratorExecutor() {
    return Executors.newFixedThreadPool(dbParallelism, new CustomizableThreadFactory("dbWorker-"));
  }

  @Bean("sleepExecutor")
  public ScheduledExecutorService getSleepExecutor() {
    return Executors.newSingleThreadScheduledExecutor(new CustomizableThreadFactory("sleepWorker-"));
  }
}
