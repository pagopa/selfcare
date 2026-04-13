package it.pagopa.selfcare.onboarding.exception;


public class UnauthorizedUserException extends RuntimeException {
  public UnauthorizedUserException(String message) {
    super(message);
  }
}
