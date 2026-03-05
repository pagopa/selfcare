package it.pagopa.selfcare.onboarding.exception;

public class NotificationException extends RuntimeException {

    private final String code;

    public NotificationException(String message, String code, Exception exception) {
        super(message, exception);
        this.code = code;
    }

    public NotificationException(String message, Exception exception) {
        super(message, exception);
        this.code = "0000";
    }

    public NotificationException(String message) {
        super(message);
        this.code = "0000";
    }

    public String getCode() {
        return code;
    }
}
