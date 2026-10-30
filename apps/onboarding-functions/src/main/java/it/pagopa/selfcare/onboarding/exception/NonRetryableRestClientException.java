package it.pagopa.selfcare.onboarding.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Definitive client error (e.g. 400, 422): retrying the same request cannot succeed. Same shape as
 * {@link RestClientException}, kept as a sibling (not a subclass) to limit the inheritance depth.
 */
public class NonRetryableRestClientException extends WebApplicationException {

  public NonRetryableRestClientException(String message, Response response) {
    super(message, response);
  }
}
