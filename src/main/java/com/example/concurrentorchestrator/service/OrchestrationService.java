package com.example.concurrentorchestrator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.function.Predicate.not;

@Slf4j
@Service
public class OrchestrationService {

  @Autowired private ApiService apiService;
  @Autowired private DbService dbService;
  @Autowired private CommunicationService communicationService;
  @Value("${api.orchestration.timeoutSeconds}") private long orchestrationTimeout;
  @Value("${api.orchestration.threshold}") private long threshold;
  @Autowired @Qualifier("dbExecutor") private Executor dbExecutor;
  @Autowired @Qualifier("apiExecutor") private Executor apiExecutor;
  @Autowired @Qualifier("sleepExecutor") private ScheduledExecutorService sleepExecutor;

  public CompletionStage<Set<String>> orchestrate(Set<String> words) {
    return Optional.of(words)
      .map(Set::size)
      .filter(size -> size > threshold)
      .map(tooBig -> orchestrateNames(words))
      .orElseGet(() -> processNamesIndividually(words));
  }

  private CompletionStage<Set<String>> orchestrateNames(Set<String> words) {

    CompletionStage<Set<String>> dbPromise = sleepPromise()
      .thenCombineAsync(supplyAsync(() ->
          dbService.getNames(words), dbExecutor).exceptionally(th -> null),
        (notRelevant, dto) -> dto, dbExecutor);

    CompletionStage<Set<String>> apiPromise = synchronizePromises(words, name ->
      buildPromise(name, apiService::checkApi, this::handleApiResult, apiExecutor));

    return resolve(apiPromise, dbPromise, result -> handleResponses(apiPromise, dbPromise, not(Set::isEmpty)));
  }

  private CompletionStage<Set<String>> processNamesIndividually(Set<String> words) {
    return synchronizePromises(words, this::orchestrateName);
  }

  private CompletionStage<String> orchestrateName(String word) {

    CompletionStage<String> apiPromise = buildPromise(word,
      apiService::checkApi, this::handleApiResult, apiExecutor);

    CompletionStage<String> dbPromise = sleepPromise()
      .thenCombineAsync(buildPromise(word, dbService::getName,
        (dto, th) -> sendErrorIfNeeded(word, th), dbExecutor), (notRelevant, str) -> str, dbExecutor);

    return resolve(apiPromise, dbPromise, result -> handleResponses(apiPromise, dbPromise, Objects::nonNull));
  }

  private <E, W, R extends Set<E>> CompletionStage<Set<W>> synchronizePromises(R source, Function<E, CompletionStage<W>> mapper) {
    CompletableFuture<W>[] promises = source.stream()
      .parallel()
      .map(mapper)
      .map(CompletionStage::toCompletableFuture)
      .toArray((IntFunction<CompletableFuture<W>[]>) CompletableFuture[]::new);

    return allOf(promises)
      .thenApply(v -> Stream.of(promises)
        .parallel()
        .map(CompletableFuture::join)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet()));
  }

  private <T, R extends CompletionStage<T>> CompletionStage<T> resolve(R apiPromise, R dbPromise, Function<T, R> handler) {
    return dbPromise
      .applyToEither(apiPromise, handler)
      .thenCompose(Function.identity());
  }

  private <W, S> CompletionStage<W> buildPromise(S word, Function<S, CompletionStage<W>> generator,
                                                 BiConsumer<W, Throwable> finisher, Executor executor) {
    return supplyAsync(() -> generator.apply(word).whenCompleteAsync(finisher, executor), executor)
      .thenComposeAsync(Function.identity(), executor)
      .exceptionally(th -> null);
  }

  private <T, E extends CompletionStage<T>> E handleResponses(E apiPromise, E dbPromise, Predicate<T> checker) {
    return Optional.of(apiPromise.toCompletableFuture())
      .filter(CompletableFuture::isDone)
      .map(CompletableFuture::join)
      .filter(checker)
      .stream()
      .peek(response -> log.info("Got successful response from API: {}", response))
      .map(validApiResponse -> apiPromise)
      .findFirst()
      .orElse(dbPromise);
  }

  private CompletableFuture<?> sleepPromise() {
    CompletableFuture<String> sleepPromise = new CompletableFuture<>();
    sleepExecutor.schedule(() -> sleepPromise.complete("sleep finished"), orchestrationTimeout, TimeUnit.SECONDS);
    return sleepPromise;
  }

  private void handleApiResult(String result, Throwable throwable) {
    Optional.ofNullable(result)
      .ifPresentOrElse(response ->
          runAsync(() -> communicationService.sendResult(response))
            .whenComplete((v, th) -> sendErrorIfNeeded(result, th)),
        () -> sendErrorIfNeeded(result, throwable));
  }

  private void sendErrorIfNeeded(String name, Throwable throwable) {
    Optional.ofNullable(throwable)
      .ifPresent(th -> runAsync(() -> communicationService.onError(name, th.getMessage())));
  }
}
