package com.example.concurrentorchestrator.service;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.CompletionStage;

@Service
public class DbService {

  public CompletionStage<String> getName(String name) {
    return null;
  }

  public Set<String> getNames(Set<String> names) {
    return null;
  }
}
