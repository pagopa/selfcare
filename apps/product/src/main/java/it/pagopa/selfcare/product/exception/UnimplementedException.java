package it.pagopa.selfcare.product.exception;

public class UnimplementedException extends RuntimeException {
  private final String code;

  public UnimplementedException(String message, String code) {
    super(message);
    this.code = code;
  }

  public UnimplementedException(String message) {
    super(message);
    this.code = "0000";
  }

  public String getCode() {
    return code;
  }
}
