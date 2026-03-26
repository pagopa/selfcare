package it.pagopa.selfcare.onboarding.connector.exceptions;


public class UnauthorizedUserException extends RuntimeException {
  public UnauthorizedUserException(String message) {
    super(message);
  }
}
