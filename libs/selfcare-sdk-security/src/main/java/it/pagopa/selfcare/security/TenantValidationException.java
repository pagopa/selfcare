package it.pagopa.selfcare.security;

public class TenantValidationException extends RuntimeException {

  public TenantValidationException() {
    super("Invalid tenant context");
  }
}
