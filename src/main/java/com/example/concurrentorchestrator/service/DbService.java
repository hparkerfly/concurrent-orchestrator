package com.example.concurrentorchestrator.service;

import com.example.concurrentorchestrator.util.Unchecked;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class DbService {

  public CompletionStage<String> getName(String word) {

    long wait = ThreadLocalRandom.current().nextInt(1, 2);
    return Optional.of(wait)
      .stream()
      .peek(Unchecked.consumer(TimeUnit.SECONDS::sleep))
      .findFirst()
      .map(sleepFinished -> word.toUpperCase())
      .map(CompletableFuture::completedFuture)
      .orElseThrow(RuntimeException::new);
  }

  public Set<String> getNames(Set<String> words) {

    return words.stream()
      .parallel()
      .map(String::toUpperCase)
      .collect(Collectors.toSet());
  }
}
