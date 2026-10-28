package it.pagopa.selfcare.onboarding.exception;

public class DatabaseThrottledException extends RuntimeException {

    private final long retryAfterMs;

    public DatabaseThrottledException(String message, long retryAfterMs, Throwable cause) {
        super(message, cause);
        this.retryAfterMs = retryAfterMs;
    }

    public long getRetryAfterMs() {
        return retryAfterMs;
    }
}
