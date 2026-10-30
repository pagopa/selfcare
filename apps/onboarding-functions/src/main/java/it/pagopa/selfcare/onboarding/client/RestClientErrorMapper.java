package it.pagopa.selfcare.onboarding.client;

import it.pagopa.selfcare.onboarding.exception.NonRetryableRestClientException;
import it.pagopa.selfcare.onboarding.exception.RestClientException;
import jakarta.ws.rs.core.Response;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

/**
 * Replaces the default "Unknown error, status code N" with a {@link RestClientException} carrying
 * status and response body. Registered on every generated client via
 * {@code additional-api-type-annotations} in application.properties.
 */
public class RestClientErrorMapper implements ResponseExceptionMapper<RestClientException> {

  static final int MAX_BODY_LENGTH = 1000;
  private static final Set<Integer> NON_RETRYABLE_STATUSES = Set.of(400, 422);

  @Override
  public RestClientException toThrowable(Response response) {
    int status = response.getStatus();
    String message = "HTTP %d - %s".formatted(status, StringUtils.abbreviate(readBody(response), MAX_BODY_LENGTH));
    return NON_RETRYABLE_STATUSES.contains(status)
        ? new NonRetryableRestClientException(message, response)
        : new RestClientException(message, response);
  }

  private static String readBody(Response response) {
    try {
      response.bufferEntity();
      return response.hasEntity() ? StringUtils.defaultString(response.readEntity(String.class)) : "";
    } catch (RuntimeException e) {
      return "<body not readable>";
    }
  }
}
