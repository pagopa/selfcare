package it.pagopa.selfcare.onboarding.service;

import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import java.util.Map;

public interface TelemetryService {

  void trackFunction(
      String functionName,
      String message,
      SeverityLevel severityLevel,
      Map<String, String> properties);

  void trackEvent(String eventName, Map<String, String> properties, Map<String, Double> metrics);
}
