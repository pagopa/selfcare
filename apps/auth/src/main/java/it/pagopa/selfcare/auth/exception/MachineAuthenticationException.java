package it.pagopa.selfcare.auth.exception;

public class MachineAuthenticationException extends RuntimeException {
  public MachineAuthenticationException() {
    super("Invalid machine credential");
  }
}
