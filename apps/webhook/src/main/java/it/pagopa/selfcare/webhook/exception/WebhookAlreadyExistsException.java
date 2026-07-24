package it.pagopa.selfcare.webhook.exception;

public class WebhookAlreadyExistsException extends RuntimeException {

  public WebhookAlreadyExistsException(String message) {
    super(message);
  }
}
