package it.pagopa.selfcare.onboarding.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Error returned by a downstream REST service. It keeps the original (buffered) {@link Response},
 * so callers can still inspect {@code getResponse().getStatus()}, and exposes status and body in
 * the message to make Azure Functions failures self-explanatory.
 */
public class RestClientException extends WebApplicationException {

  public RestClientException(String message, Response response) {
    super(message, response);
  }
}
