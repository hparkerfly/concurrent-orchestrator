package com.example.concurrentorchestrator.controller;

import com.example.concurrentorchestrator.model.InputDto;
import com.example.concurrentorchestrator.model.OutputDto;
import com.example.concurrentorchestrator.service.OrchestrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Slf4j
@RestController
@RequestMapping("names")
public class NameController {

  @Autowired private OrchestrationService orchestrationService;

  @PostMapping
  public CompletionStage<ResponseEntity<OutputDto>> getNames(@RequestBody InputDto inputDto) {

    return Optional.of(inputDto)
      .map(InputDto::getNames)
      .map(orchestrationService::orchestrate)
      .map(result -> result.thenApply(OutputDto::new))
      .map(result -> result.thenApply(ResponseEntity::ok).exceptionally(this::handleError))
      .orElseGet(() -> CompletableFuture.completedFuture(ResponseEntity.noContent().build()));
  }

  private ResponseEntity<OutputDto> handleError(Throwable throwable) {
    log.error("Exception thrown when transforming names", throwable.getCause());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
  }
}