package com.example.concurrentorchestrator.util;

import java.util.function.Consumer;

public interface Unchecked {

  static <T> Consumer<T> consumer(ThrowingConsumer<T> throwingConsumer) {
    return t -> {
      try {
        throwingConsumer.accept(t);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }
}
