package com.example.concurrentorchestrator.model;

import java.util.Set;

public class OutputDto {

  Set<String> names;

  public OutputDto(Set<String> names) {
    this.names = names;
  }

  public Set<String> getNames() {
    return names;
  }

  public void setNames(Set<String> names) {
    this.names = names;
  }
}
