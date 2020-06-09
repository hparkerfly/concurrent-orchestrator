package com.example.concurrentorchestrator.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.CompletionStage;

@Service
public class ApiService {

  public CompletionStage<String> checkApi(String name) {
    return null;
  }
}
