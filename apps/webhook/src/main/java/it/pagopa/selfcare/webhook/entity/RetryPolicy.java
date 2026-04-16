package it.pagopa.selfcare.webhook.entity;

import lombok.Data;

@Data
public class RetryPolicy {

  private Integer maxAttempts;
  private Long initialDelayMs;
  private Long maxDelayMs;
  private Double backoffMultiplier;

  public RetryPolicy() {
    this.maxAttempts = 3;
    this.initialDelayMs = 1000L;
    this.maxDelayMs = 10000L;
    this.backoffMultiplier = 2.0;
  }
}
