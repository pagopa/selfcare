package it.pagopa.selfcare.onboarding.exception;

/**
 * Raised when a product declares a dedicated database that cannot be resolved.
 *
 * <p>This is deliberately an error rather than a fallback: routing such a product to the shared
 * database would write its data into a database it was explicitly configured out of.
 */
public class UnresolvableProductDatabaseException extends RuntimeException {

  public UnresolvableProductDatabaseException(String message) {
    super(message);
  }
}
