package it.pagopa.selfcare.onboarding.exception;

import jakarta.ws.rs.core.Response;

/** Definitive client error (e.g. 400, 422): retrying the same request cannot succeed. */
public class NonRetryableRestClientException extends RestClientException {

  public NonRetryableRestClientException(String message, Response response) {
    super(message, response);
  }
}
