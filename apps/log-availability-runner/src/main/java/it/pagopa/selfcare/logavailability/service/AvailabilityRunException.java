package it.pagopa.selfcare.logavailability.service;

public class AvailabilityRunException extends RuntimeException {

    private final Stage stage;

    public AvailabilityRunException(Stage stage, Throwable cause) {
        super("Availability run failed during " + stage, cause);
        this.stage = stage;
    }

    public Stage stage() {
        return stage;
    }

    public enum Stage {
        SOURCE_QUERY,
        TABLE_STORAGE_WRITE,
        LOG_ANALYTICS_WRITE
    }
}
