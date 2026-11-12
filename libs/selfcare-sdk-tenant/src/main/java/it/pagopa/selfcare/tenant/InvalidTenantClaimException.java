package it.pagopa.selfcare.tenant;

/** The {@code tenant_id} claim of a verified JWT is missing (when required), blank or unsupported. */
public class InvalidTenantClaimException extends RuntimeException {

  public InvalidTenantClaimException(String message) {
    super(message);
  }
}
