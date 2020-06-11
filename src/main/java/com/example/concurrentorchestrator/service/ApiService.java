package com.example.concurrentorchestrator.service;

import com.example.concurrentorchestrator.util.Unchecked;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class ApiService {

  public CompletionStage<String> checkApi(String word) {

    long wait = ThreadLocalRandom.current().nextInt(3, 5);
    return Optional.of(wait)
      .stream()
      .peek(Unchecked.consumer(TimeUnit.SECONDS::sleep))
      .findFirst()
      .map(sleepFinished -> word.toUpperCase())
      .map(CompletableFuture::completedFuture)
      .orElseThrow(RuntimeException::new);
  }
}
