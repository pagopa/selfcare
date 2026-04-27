package it.pagopa.selfcare.onboarding.service;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * Service for telemetry tracking operations.
 * This service encapsulates the TelemetryClient and provides methods for tracking function execution.
 */
@ApplicationScoped
public class TelemetryService {

    @Inject
    TelemetryClient telemetryClient;

    /**
     * Track a function execution with telemetry.
     *
     * @param functionName the name of the function being tracked
     * @param message the message to log
     * @param severityLevel the severity level of the log
     * @param properties additional properties to include in the telemetry
     */
    public void trackFunction(
            String functionName,
            String message,
            SeverityLevel severityLevel,
            Map<String, String> properties) {
        if (telemetryClient != null) {
            telemetryClient.getContext().getOperation().setName(functionName);
            telemetryClient.trackTrace(message, severityLevel, properties);
        }
    }

    /**
     * Track an event with telemetry.
     *
     * @param eventName the name of the event being tracked
     * @param properties additional properties to include in the telemetry
     * @param metrics metrics to include in the telemetry
     */
    public void trackEvent(
            String eventName,
            Map<String, String> properties,
            Map<String, Double> metrics) {
        if (telemetryClient != null) {
            telemetryClient.trackEvent(eventName, properties, metrics);
        }
    }
}

