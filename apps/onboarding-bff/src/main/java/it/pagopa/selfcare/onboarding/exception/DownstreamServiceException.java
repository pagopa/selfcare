package it.pagopa.selfcare.onboarding.exception;

/**
 * Downstream answer with a status that carries no dedicated BFF exception (e.g. 401, 403, 422, 429).
 * It is deliberately not a {@link jakarta.ws.rs.WebApplicationException}: the downstream response has
 * already been consumed, and the server runtime would otherwise inspect the closed response.
 */
public class DownstreamServiceException extends RuntimeException {

    private final int status;

    public DownstreamServiceException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
