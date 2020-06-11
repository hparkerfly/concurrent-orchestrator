package com.example.concurrentorchestrator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CommunicationService {

  public void sendResult(String word) {
    log.info("Sending translated word to DB");
  }

  public void onError(String word, String errorMessage) {
    log.error("Word '{}' translation has finished in error {}", word, errorMessage);
  }
}
